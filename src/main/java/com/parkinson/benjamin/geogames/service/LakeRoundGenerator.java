package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.Lake;
import com.parkinson.benjamin.geogames.model.LakeRound;
import com.parkinson.benjamin.geogames.model.SubjectIds;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Makes the rounds of {@link GameType#LAKES_BY_SHAPE}. If lake variants are ever added, turn this
 * into a parameterised class registered as several beans, as the rivers did.
 */
@Service
public class LakeRoundGenerator extends SubjectRoundGenerator<Lake> {

  private final LakeLoaderService lakeLoaderService;

  public LakeRoundGenerator(LakeLoaderService lakeLoaderService) {
    super(GameType.LAKES_BY_SHAPE, SubjectIds.LAKE, "lakes");
    this.lakeLoaderService = lakeLoaderService;
  }

  @Override
  List<Lake> candidates() {
    return lakeLoaderService.loadLakes();
  }

  @Override
  String id(Lake lake) {
    return lake.id();
  }

  @Override
  Optional<Lake> find(String id) {
    return lakeLoaderService.loadLakes().stream().filter(lake -> lake.id().equals(id)).findFirst();
  }

  @Override
  GameData toRound(Lake lake) {
    return new LakeRound(GameType.LAKES_BY_SHAPE, lake);
  }
}
