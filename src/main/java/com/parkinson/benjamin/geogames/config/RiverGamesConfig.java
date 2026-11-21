package com.parkinson.benjamin.geogames.config;

import com.parkinson.benjamin.geogames.helper.RiverCountriesEligibility;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.CountryShapeService;
import com.parkinson.benjamin.geogames.service.RiverLoaderService;
import com.parkinson.benjamin.geogames.service.RiverRoundGenerator;
import com.parkinson.benjamin.geogames.service.RoundGenerator;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One round generator per river game type; they all use the same rivers. To remove a variant,
 * delete its bean, its {@code GameType} constant and its frontend module.
 */
@Configuration
public class RiverGamesConfig {

  @Bean
  RoundGenerator riversByShape(RiverLoaderService rivers) {
    return new RiverRoundGenerator(
        GameType.RIVERS_BY_SHAPE, rivers, river -> true, river -> List.of());
  }

  /** Only rivers through enough countries to name them: see {@link RiverCountriesEligibility}. */
  @Bean
  RoundGenerator riversByCountries(RiverLoaderService rivers, CountryShapeService shapes) {
    return new RiverRoundGenerator(
        GameType.RIVERS_BY_COUNTRIES,
        rivers,
        RiverCountriesEligibility.riverCountries(),
        river -> river.countries().stream().map(shapes::loadDisplayCountry).toList());
  }
}
