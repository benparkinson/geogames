package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.RandomPicker;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.Lake;
import com.parkinson.benjamin.geogames.model.LakeRound;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Makes the rounds of {@link GameType#LAKES_BY_SHAPE}. If lake variants are ever added, turn this
 * into a parameterised class registered as several beans, as the rivers did.
 */
@Service
public class LakeRoundGenerator implements RoundGenerator {

  private final LakeLoaderService lakeLoaderService;
  private final Random random;

  public LakeRoundGenerator(LakeLoaderService lakeLoaderService, Random random) {
    this.lakeLoaderService = lakeLoaderService;
    this.random = random;
  }

  @Override
  public GameType gameType() {
    return GameType.LAKES_BY_SHAPE;
  }

  @Override
  public List<GameData> generate(int count, Set<String> excludedSubjects) {
    List<Lake> candidates =
        lakeLoaderService.loadLakes().stream()
            .filter(lake -> !excludedSubjects.contains("lake:" + lake.id()))
            .toList();
    return RandomPicker.pickDistinct(candidates, count, random, "lakes").stream()
        .<GameData>map(lake -> new LakeRound(GameType.LAKES_BY_SHAPE, lake))
        .toList();
  }
}
