package com.example.server.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.example.admin.permission.context.AuthContextTaskDecorator;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public TaskDecorator authContextTaskDecorator() {
        return new AuthContextTaskDecorator();
    }

    @Bean(name = "applicationTaskExecutor")
    public Executor applicationTaskExecutor(TaskDecorator authContextTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("async-exec-");
        executor.setTaskDecorator(authContextTaskDecorator);
        executor.initialize();
        return executor;
    }
}
