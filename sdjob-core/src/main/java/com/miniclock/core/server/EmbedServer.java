package com.miniclock.core.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * @author strind
 * @date 2024/8/24 8:00
 * @description
 */
public class EmbedServer {

    public static final Logger logger = LoggerFactory.getLogger(EmbedServer.class);

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
        )

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
                            .addLast(new embedHttpServerHandler(bizThreadPool));
                    }
                })
                .childOption(ChannelOption.SO_KEEPALIVE,true);
            ChannelFuture future = bootstrap.bind(port).sync();
            // 注册执行器到调度中心
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
    }

    public void stop() {

    }
}
