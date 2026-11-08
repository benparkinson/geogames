package com.parkinson.benjamin.geogames.config;

import com.parkinson.benjamin.geogames.helper.CountryShapeEligibility;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.CountryShapeRoundGenerator;
import com.parkinson.benjamin.geogames.service.CountryShapeService;
import com.parkinson.benjamin.geogames.service.RoundGenerator;
import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One round generator per country-shape game type; they all use the same simplified country
 * shapes. The eligibility thresholds, with the reason for each, are in {@link
 * CountryShapeEligibility}. To remove a variant, delete its bean, its {@code GameType} constant and
 * its frontend module.
 */
@Configuration
public class CountryGamesConfig {

  @Bean
  RoundGenerator countriesByNeighbours(CountryShapeService shapes, Random random) {
    return new CountryShapeRoundGenerator(
        GameType.COUNTRIES_BY_NEIGHBOURS,
        shapes,
        random,
        CountryShapeEligibility.neighbours(),
        true);
  }
}
