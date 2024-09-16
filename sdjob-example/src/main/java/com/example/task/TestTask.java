package com.example.task;

import com.miniclock.core.content.SdJobHelper;
import com.miniclock.core.handler.annotation.SdJob;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author strind
 * @date 2024/8/25 17:18
 * @description
 */
@Component
public class TestTask {
    private AtomicInteger atomicInteger = new AtomicInteger(0);

    @SdJob(value = "TestHandler")
    public void test(){
        System.out.println("这是第" + atomicInteger.incrementAndGet()+ "被调用");
    }

    /**
     * 2、分片广播任务
     */
    @SdJob("shardingJobHandler")
    public void shardingJobHandler() throws Exception {

        // 分片参数
        int shardIndex = SdJobHelper.getShardIndex();
        int shardTotal = SdJobHelper.getShardTotal();
        System.out.println("分片参数"+shardIndex+"++++++++++++"+shardTotal);

        SdJobHelper.log("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

        // 业务逻辑
        for (int i = 0; i < shardTotal; i++) {
            if (i == shardIndex) {
                SdJobHelper.log("第 {} 片, 命中分片开始处理", i);
            } else {
                SdJobHelper.log("第 {} 片, 忽略", i);
            }
        }

    }

}
