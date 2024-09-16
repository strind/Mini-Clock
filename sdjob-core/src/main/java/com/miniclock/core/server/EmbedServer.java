package com.miniclock.core.server;

import com.miniclock.core.biz.ExecutorBiz;
import com.miniclock.core.biz.impl.ExecutorBizImpl;
import com.miniclock.core.biz.model.*;
import com.miniclock.core.thread.ExecutorRegistryThread;
import com.miniclock.core.util.GsonTool;
import com.miniclock.core.util.SdJobRemotingUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * @author strind
 * @date 2024/8/24 8:00
 * @description 执行器端的内置Netty
 */
public class EmbedServer {

    public static final Logger logger = LoggerFactory.getLogger(EmbedServer.class);

    // 执行器的接口
    private ExecutorBiz executorBiz;

    // 启动Netty的线程
    private Thread work;

    public void start(final String address, final int port, final String appName, String accessToken) {
        ThreadPoolExecutor bizThreadPool = new ThreadPoolExecutor(
            0,
            200,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2000),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "sdJob, EmbedServer bizThreadPool-" + r.hashCode());
                }
            },
            new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    throw new RuntimeException("sdJob, EmbedServer bizThreadPool is EXHAUSTED!");
                }
            }
        );

        // 用于执行定时任务
        executorBiz = new ExecutorBizImpl();
        work = new Thread(()->{
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup,workGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                // 心跳检测
                                .addLast(new IdleStateHandler(0,0,30*3, TimeUnit.SECONDS))
                                .addLast(new HttpServerCodec())
                                // 消息聚合，将拆分传递的消息合在一起
                                .addLast(new HttpObjectAggregator(5*1024*1024))
                                .addLast(new EmbedHttpServerHandler(executorBiz, accessToken,bizThreadPool));
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE,true);
                ChannelFuture future = bootstrap.bind(port).sync();
                // 注册执行器到调度中心
                logger.info("register to {} by appName {}", address, appName);
                startRegistry(appName, address);
                // 等待关闭
                future.channel().closeFuture().sync();
            }catch (InterruptedException e){
                logger.info(">>>>>>>>>>>>>>>>>> sdJob remoting server stop..");
            }finally {
                try {
                    // 优雅关闭
                    workGroup.shutdownGracefully();
                    bossGroup.shutdownGracefully();
                }catch (Exception e){
                    logger.error(e.getMessage(),e);
                }
            }
        });
        work.setDaemon(true);
        work.start();
        logger.info("SdJob EmbedServer started....");
    }

    public void stop() {
        if (work != null && work.isAlive()){
            work.interrupt();
        }
        stopRegistry();
        logger.info(">>>>>>>>>>> SdJob remoting server destroy success.");
    }

    public static class EmbedHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        public static final Logger logger = LoggerFactory.getLogger(EmbedHttpServerHandler.class);

        private ExecutorBiz executorBiz;

        private String accessToken;
        private ThreadPoolExecutor bizThreadPool;
        public EmbedHttpServerHandler(ExecutorBiz executorBiz, String accessToken, ThreadPoolExecutor bizThreadPool) {
            this.accessToken = accessToken;
            this.executorBiz =  executorBiz;
            this.bizThreadPool = bizThreadPool;
        }



        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest msg)
            throws Exception {
            // requestData 就是 TriggerParam
            String requestData = msg.content().toString(CharsetUtil.UTF_8);
            // 调度中心访问Netty服务器是的uri
            String uri = msg.uri();
            HttpMethod httpMethod = msg.method();
            boolean keepAlive = HttpUtil.isKeepAlive(msg);
            String accessToken = msg.headers().get(SdJobRemotingUtil.SD_JOB_ACCESS_TOKEN);

            bizThreadPool.execute(()->{
                Object responseObj = process(httpMethod, requestData, uri,accessToken);
                String response = GsonTool.toJson(responseObj);
                writeResponse(ctx,keepAlive, response);
            });
        }

        /**
         * 该方法就是把执行的定时任务的结果发送到调度中心
         */
        private void writeResponse(ChannelHandlerContext ctx, boolean keepAlive, String responseJson) {
            //设置响应结果
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(responseJson, CharsetUtil.UTF_8));
            //设置文本类型
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");
            //消息的字节长度
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            if (keepAlive) {
                //连接是存活状态
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            //开始发送消息
            ctx.writeAndFlush(response);
        }

        private Object process(HttpMethod httpMethod, String requestData, String uri, String accessTokenReq) {
            if (HttpMethod.POST != httpMethod){
                return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support");
            }
            //校验uri是否为空
            if (uri == null || uri.trim().isEmpty()) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, uri-mapping empty.");
            }
            //判断执行器令牌是否和调度中心令牌一样，这里也能发现，调度中心和执行器的token令牌一定要是相等的，因为判断是双向的，两边都要判断
            if (accessToken != null
                && !accessToken.trim().isEmpty()
                && !accessToken.equals(accessTokenReq)) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong.");
            }
            try {
                switch (uri){
                    //这里触发的就是心跳检测，判断执行器这一端是否启动了
                    case "/beat":
                        return executorBiz.beat();
                    case "/idleBeat":
                        //这里就是判断调度中心要调度的任务是否可以顺利执行，其实就是判断该任务是否正在被
                        //执行器这一端执行或者在执行器的队列中，如果在的话，说明当前执行器比较繁忙
                        IdleBeatParam idleBeatParam = GsonTool.fromJson(requestData, IdleBeatParam.class);
                        return executorBiz.idleBeat(idleBeatParam);
                    case "/run":
                        //run就意味着是要执行定时任务
                        //把requestData转化成触发器参数对象，也就是TriggerParam对象
                        TriggerParam triggerParam = GsonTool.fromJson(requestData, TriggerParam.class);
                        //然后交给ExecutorBizImpl对象去执行定时任务
                        return executorBiz.run(triggerParam);
                    //走到这个分支就意味着要终止任务
                    case "/kill":
                        KillParam killParam = GsonTool.fromJson(requestData, KillParam.class);
                        return executorBiz.kill(killParam);
                    case "/log":
                        //远程访问执行器端日志
                        LogParam logParam = GsonTool.fromJson(requestData, LogParam.class);
                        return executorBiz.log(logParam);
                    default:
                        return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, uri-mapping(" + uri + ") not found.");
                }
            }catch (Exception e){
                logger.error(e.getMessage(),e);
                return new ReturnT<String>(ReturnT.FAIL_CODE,"request error: " + ThrowableUtil.stackTraceToString(e));
            }

        }

        /**
         * Netty中入站处理器的方法的回调
         */
        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error(">>>>>>>>>>> xxl-job provider netty_http server caught exception", cause);
            ctx.close();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                ctx.channel().close();
                logger.debug(">>>>>>>>>>> xxl-job provider netty_http server close an idle channel.");
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }

    // 启动注册线程，将执行器注册到调度中心
    public void startRegistry(final String appName, final String address){
        ExecutorRegistryThread.getInstance().start(appName,address);
    }
    public void stopRegistry(){
        ExecutorRegistryThread.getInstance().toStop();
    }

}
