package com.parkinson.benjamin.geogames.config;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * The executor the round statistics are written on: one thread, so at most one database connection,
 * and a bounded queue. A full queue drops the row rather than slowing the game. Nothing else in the
 * app is {@code @Async}.
 */
@Configuration
@EnableAsync
public class StatsConfig {

  private static final Logger log = LoggerFactory.getLogger(StatsConfig.class);

  @Bean(name = "statsExecutor")
  public Executor statsExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(1);
    executor.setQueueCapacity(1000);
    executor.setThreadNamePrefix("stats-");
    executor.setRejectedExecutionHandler(
        (task, pool) -> log.warn("Dropped a round statistics row: the queue is full"));
    // So a redeploy doesn't lose the rows still queued.
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(5);
    return executor;
  }
}
