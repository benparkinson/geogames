package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

/**
 * Proves the stored round key is enough: for every generator and every round it can make from the
 * real data, rebuilding from the key gives identical JSON.
 */
@SpringBootTest
class RoundRebuildIT {

  @Autowired private List<RoundGenerator> generators;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private TripointLoaderService tripoints;
  @Autowired private CountryShapeService shapes;
  @Autowired private RiverLoaderService rivers;
  @Autowired private LakeLoaderService lakes;

  @Test
  void everyRoundOfEveryGeneratorRebuildsToIdenticalJson() {
    int total = 0;
    for (RoundGenerator generator : generators) {
      List<GameData> all = everyRound(generator);
      assertThat(all).as(generator.gameType().toString()).isNotEmpty();

      Set<String> keys = new HashSet<>();
      for (GameData round : all) {
        assertThat(keys.add(round.roundKey())).as("duplicate key " + round.roundKey()).isTrue();
        GameData rebuilt = generator.rebuild(round.roundKey());
        assertThat(rebuilt).as(round.roundKey()).isEqualTo(round);
        assertThat(objectMapper.writeValueAsString(rebuilt))
            .as(round.roundKey())
            .isEqualTo(objectMapper.writeValueAsString(round));
      }
      total += all.size();
    }
    assertThat(total).isGreaterThan(500);
  }

  /** Asks for as many rounds as there are candidates, backing off until the generator agrees. */
  private List<GameData> everyRound(RoundGenerator generator) {
    int candidates =
        switch (generator.gameType()) {
          case TRIPOINT -> tripoints.loadTripointDefinitions().size();
          case LAKES_BY_SHAPE -> lakes.loadLakes().size();
          case RIVERS_BY_SHAPE, RIVERS_BY_COUNTRIES -> rivers.loadRivers().size();
          case COUNTRIES_BY_NEIGHBOURS, COUNTRIES_BY_COASTLINE, COUNTRIES_BY_LAND_BORDERS ->
              shapes.loadShapes().size();
        };
    for (int count = candidates; count > 0; count--) {
      try {
        return generator.generate(count, Set.of());
      } catch (IllegalArgumentException tooFewCandidates) {
        // Some candidates aren't eligible for this game type: ask for fewer.
      }
    }
    throw new AssertionError("no rounds for " + generator.gameType());
  }

  @Test
  void everyGameTypeHasAGenerator() {
    assertThat(generators.stream().map(RoundGenerator::gameType))
        .containsExactlyInAnyOrder(GameType.values());
  }
}
