package com.parkinson.benjamin.geogames.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class StatsConfigTest {

  private final ThreadPoolTaskExecutor executor =
      (ThreadPoolTaskExecutor) new StatsConfig().statsExecutor();

  @Test
  void runsTasksOnOneThreadNamedStats() throws Exception {
    executor.initialize();
    AtomicReference<String> thread = new AtomicReference<>();
    CountDownLatch done = new CountDownLatch(1);

    executor.execute(
        () -> {
          thread.set(Thread.currentThread().getName());
          done.countDown();
        });

    assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(thread.get()).startsWith("stats-");
    assertThat(executor.getMaxPoolSize()).isEqualTo(1);
    executor.shutdown();
  }

  @Test
  void aFullQueueDropsTheTaskInsteadOfThrowingOrBlocking() throws Exception {
    executor.initialize();
    CountDownLatch release = new CountDownLatch(1);
    Executor stats = executor;
    // One task occupies the thread, then the queue of 1000 fills up.
    stats.execute(() -> await(release));
    for (int i = 0; i < 1000; i++) {
      stats.execute(() -> {});
    }

    assertThatCode(() -> stats.execute(() -> {})).doesNotThrowAnyException();

    release.countDown();
    executor.shutdown();
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
