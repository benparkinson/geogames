package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.RandomPicker;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class RiverFinderService implements RoundGenerator {

  private final RiverLoaderService riverLoaderService;
  private final Random random;

  public RiverFinderService(RiverLoaderService riverLoaderService, Random random) {
    this.riverLoaderService = riverLoaderService;
    this.random = random;
  }

  @Override
  public GameType gameType() {
    return GameType.RIVERS_BY_SHAPE;
  }

  @Override
  public List<GameData> generate(int count) {
    return List.copyOf(
        RandomPicker.pickDistinct(riverLoaderService.loadRivers(), count, random, "rivers"));
  }
}
