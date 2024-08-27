package com.example;

import com.miniclock.core.executor.SdJobExecutor;
import com.miniclock.core.executor.impl.SdJobSpringExecutor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author strind
 * @date 2024/8/25 17:18
 * @description
 */
@SpringBootApplication
public class ExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class);
    }

    @Bean
    public SdJobExecutor sdJobExecutor(){
        SdJobExecutor executor = new SdJobSpringExecutor();
        executor.setAdminAddress("127.0.0.1:7777");
        executor.setAppName("TestHandler");
        executor.setAccessToken("token");
        executor.setAddress("127.0.0.1:8080");
        executor.setIp("127.0.0.1");
        executor.setPort(8080);
        return executor;
    }

}
