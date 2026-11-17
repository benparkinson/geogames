package com.parkinson.benjamin.geogames.config;

import com.parkinson.benjamin.geogames.helper.CountryPairEligibility;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.CountryPairRoundGenerator;
import com.parkinson.benjamin.geogames.service.CountryShapeService;
import com.parkinson.benjamin.geogames.service.RoundGenerator;
import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One round generator per pair game type; they all use the same simplified country shapes. The
 * eligibility thresholds, with the reason for each, are in {@link CountryPairEligibility}. To
 * remove a variant, delete its bean, its {@code GameType} constant and its frontend module.
 */
@Configuration
public class CountryPairGamesConfig {

  /** The border between two countries as a lone line. */
  @Bean
  RoundGenerator bordersByShape(CountryShapeService shapes, Random random) {
    return new CountryPairRoundGenerator(
        GameType.BORDERS_BY_SHAPE, shapes, random, CountryPairEligibility.borderShapes(), false);
  }

  /** Two neighbours filled as one shape, with the countries around them for a hint. */
  @Bean
  RoundGenerator mergedCountries(CountryShapeService shapes, Random random) {
    return new CountryPairRoundGenerator(
        GameType.MERGED_COUNTRIES, shapes, random, CountryPairEligibility.mergedCountries(), true);
  }
}
