package com.parkinson.benjamin.geogames.config;

import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.CountryShapeService;
import com.parkinson.benjamin.geogames.service.RiverLoaderService;
import com.parkinson.benjamin.geogames.service.RiverRoundGenerator;
import com.parkinson.benjamin.geogames.service.RoundGenerator;
import java.util.List;
import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One round generator per river game type; they all use the same rivers. To remove a variant,
 * delete its bean, its {@code GameType} constant and its frontend module.
 */
@Configuration
public class RiverGamesConfig {

  @Bean
  RoundGenerator riversByShape(RiverLoaderService rivers, Random random) {
    return new RiverRoundGenerator(
        GameType.RIVERS_BY_SHAPE, rivers, random, river -> true, river -> List.of());
  }

  @Bean
  RoundGenerator riversOnMap(RiverLoaderService rivers, Random random) {
    return new RiverRoundGenerator(
        GameType.RIVERS_ON_MAP, rivers, random, river -> true, river -> List.of());
  }

  @Bean
  RoundGenerator riversByEnds(RiverLoaderService rivers, Random random) {
    return new RiverRoundGenerator(
        GameType.RIVERS_BY_ENDS,
        rivers,
        random,
        river -> river.source() != null && river.mouth() != null,
        river -> List.of());
  }

  /** Only rivers through three or more countries: one or two outlines are too easy or unguessable. */
  @Bean
  RoundGenerator riversByCountries(
      RiverLoaderService rivers, CountryShapeService shapes, Random random) {
    return new RiverRoundGenerator(
        GameType.RIVERS_BY_COUNTRIES,
        rivers,
        random,
        river -> river.countries() != null && river.countries().size() >= 3,
        river -> river.countries().stream().map(shapes::loadDisplayCountry).toList());
  }
}
