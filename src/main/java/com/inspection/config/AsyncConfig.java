package com.inspection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "inventoryTaskExecutor")
    public Executor inventoryTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // CPU 코어 수에 기반한 동적 스레드 풀 크기 계산
        int coreCount = Runtime.getRuntime().availableProcessors();
        int corePoolSize = Math.max(16, coreCount);
        int maxPoolSize = corePoolSize * 2;
        int queueCapacity = maxPoolSize * 20;
        
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("Inventory-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }
    
    @Bean(name = "queryTaskExecutor")
    public Executor queryTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // CPU 코어 수에 기반한 동적 스레드 풀 크기 계산
        int coreCount = Runtime.getRuntime().availableProcessors();
        int corePoolSize = coreCount;
        int maxPoolSize = coreCount * 2;
        
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(150);
        executor.setThreadNamePrefix("Query-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
} 