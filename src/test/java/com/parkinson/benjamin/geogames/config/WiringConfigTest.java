package com.parkinson.benjamin.geogames.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.zalando.logbook.Strategy;
import org.zalando.logbook.core.WithoutBodyStrategy;

/**
 * The small configuration classes that are only bean factories. The application context test runs
 * them too, but the coverage gate counts unit tests only, and each one guards something that would
 * otherwise fail quietly.
 */
class WiringConfigTest {

  @Test
  void logsRequestsWithoutTheirBodiesBecauseTheResponsesHoldWholeCountries() {
    Strategy strategy = new LogConfig().strategy();

    assertThat(strategy).isInstanceOf(WithoutBodyStrategy.class);
  }

  @Test
  void cachingStaysEnabledOrTheLoadersWouldReadTheFilesOnEveryRequest() {
    assertThat(new CachingConfig().getClass().isAnnotationPresent(EnableCaching.class)).isTrue();
  }

  @Test
  void schedulingStaysEnabledOrOldGamesWouldNeverBeDeleted() {
    assertThat(new SchedulingConfig().getClass().isAnnotationPresent(EnableScheduling.class))
        .isTrue();
  }

  @Test
  void providesARandomThatCanPickRounds() {
    var random = new RandomConfig().random();

    assertThat(random.nextInt(10)).isBetween(0, 9);
    assertThat(new RandomConfig().random()).isNotSameAs(random);
  }
}
