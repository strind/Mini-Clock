package com.miniclock.core.server;

import com.miniclock.core.executor.SdJobExecutor;
import com.miniclock.core.handler.JobHandler;
import com.miniclock.core.model.ReturnT;
import com.miniclock.core.model.TriggerParam;
import com.miniclock.core.thread.JobThread;
import com.miniclock.core.util.GsonTool;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author strind
 * @date 2024/8/24 8:16
 * @description
 */
public class embedHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static final Logger logger = LoggerFactory.getLogger(embedHttpServerHandler.class);

    private ThreadPoolExecutor bizThreadPool = null;
    public embedHttpServerHandler(ThreadPoolExecutor bizThreadPool) {
        this.bizThreadPool = bizThreadPool;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest msg)
        throws Exception {
        // requestData 就是 TriggerParam
        String requestData = msg.content().toString(CharsetUtil.UTF_8);
        HttpMethod httpMethod = msg.method();
        boolean keepAlive = HttpUtil.isKeepAlive(msg);
        bizThreadPool.execute(()->{
            Object responseObj = process(httpMethod, requestData);
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

    private Object process(HttpMethod httpMethod, String requestData) {
        if (HttpMethod.POST != httpMethod){
            return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support");
        }
        try {
            TriggerParam triggerParam = GsonTool.fromJson(requestData, TriggerParam.class);
            JobThread jobThread = SdJobExecutor.loadJobThread(triggerParam.getJobId());
            if (jobThread != null){
                return jobThread.pushTriggerParam(triggerParam);
            }

            String jobName = triggerParam.getExecutorName();
            // 获取具体的执行器
            JobHandler jobHandler = SdJobExecutor.loadJobHandler(jobName);
            JobThread thread = SdJobExecutor.regisJobThread(triggerParam.getJobId(), jobHandler);
            return thread.pushTriggerParam(triggerParam);
        }catch (Exception e){
            logger.error(e.getMessage(),e);
            return new ReturnT<String>(ReturnT.FAIL_CODE,"request error: " + ThrowableUtil.stackTraceToString(e));
        }

    }
}
