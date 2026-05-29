package com.hud;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulingConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("hud-scheduling-");
        scheduler.initialize();
        return scheduler;
    }

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "hud.scheduling.enabled", havingValue = "true", matchIfMissing = true)
    public static class SchedulingEnabler {
    }
}
