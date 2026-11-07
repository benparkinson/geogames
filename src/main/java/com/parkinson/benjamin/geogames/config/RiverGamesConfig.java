package com.parkinson.benjamin.geogames.config;

import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.RiverLoaderService;
import com.parkinson.benjamin.geogames.service.RiverRoundGenerator;
import com.parkinson.benjamin.geogames.service.RoundGenerator;
import java.util.List;
import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** One round generator per river game type; they all use the same rivers. */
@Configuration
public class RiverGamesConfig {

  @Bean
  RoundGenerator riversByShape(RiverLoaderService rivers, Random random) {
    return new RiverRoundGenerator(
        GameType.RIVERS_BY_SHAPE, rivers, random, river -> true, river -> List.of());
  }
}
