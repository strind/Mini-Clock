package com.example.task;

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


}
