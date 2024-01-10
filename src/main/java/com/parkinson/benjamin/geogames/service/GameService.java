package com.parkinson.benjamin.geogames.service;

import static com.parkinson.benjamin.geogames.helper.OptionalHelper.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkinson.benjamin.geogames.dao.GameData;
import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.dao.GameType;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.GameCreationResponse;
import com.parkinson.benjamin.geogames.model.GameRound;
import com.parkinson.benjamin.geogames.model.River;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GameService {

  private static final int MAX_ROUNDS = 10;

  private final GameRepository gameRepository;
  private final CountryLoaderService countryLoaderService;
  private final TripointFinderService tripointFinderService;
  private final RiverLoaderService riverLoaderService;
  private final RiverFinderService riverFinderService;
  private final ObjectMapper objectMapper;

  @Autowired
  public GameService(
      GameRepository gameRepository,
      CountryLoaderService countryLoaderService,
      TripointFinderService tripointFinderService,
      RiverLoaderService riverLoaderService,
      RiverFinderService riverFinderService,
      ObjectMapper objectMapper) {
    this.gameRepository = gameRepository;
    this.countryLoaderService = countryLoaderService;
    this.tripointFinderService = tripointFinderService;
    this.riverLoaderService = riverLoaderService;
    this.riverFinderService = riverFinderService;
    this.objectMapper = objectMapper;
  }

  public Optional<GameCreationResponse> createGame(GameType gameType, int numberOfRounds)
      throws IOException {
    if (numberOfRounds < 1 || numberOfRounds > MAX_ROUNDS) {
      throw new IllegalArgumentException(
          "Number of rounds must be between 1 and %d, was %d!"
              .formatted(MAX_ROUNDS, numberOfRounds));
    }

    if (gameType == GameType.TRIPOINT) {

      List<Country> countries = countryLoaderService.loadCountries();
      List<GameData> randomTripoints =
          tripointFinderService.findRandomTripoints(countries, numberOfRounds);

      List<GameRoundEntity> gameRoundEntities = new ArrayList<>(randomTripoints.size());
      for (int i = 0; i < randomTripoints.size(); i++) {
        gameRoundEntities.add(
            new GameRoundEntity(i, objectMapper.writeValueAsString(randomTripoints.get(i))));
      }

      GameEntity newGameEntity = new GameEntity(gameType, gameRoundEntities);
      GameEntity saved = gameRepository.save(newGameEntity);

      return Optional.of(new GameCreationResponse(saved.getId()));
    }

    if (gameType == GameType.RIVERS_BY_SHAPE) {

      List<River> rivers = riverLoaderService.loadRivers();
      List<River> randomRivers = riverFinderService.findRandomRivers(rivers, numberOfRounds);
      List<GameRoundEntity> gameRoundEntities = new ArrayList<>(randomRivers.size());

      for (int i = 0; i < randomRivers.size(); i++) {
        gameRoundEntities.add(
            new GameRoundEntity(i, objectMapper.writeValueAsString(randomRivers.get(i))));
      }

      GameEntity newGameEntity = new GameEntity(gameType, gameRoundEntities);
      GameEntity saved = gameRepository.save(newGameEntity);

      return Optional.of(new GameCreationResponse(saved.getId()));
    }

    return Optional.empty();
  }

  public Optional<GameRound> getGameRound(long gameId, int round) {
    Optional<GameEntity> game = gameRepository.findById(gameId);
    Optional<GameRoundEntity> gameRound =
        game.flatMap(g -> g.getRounds().stream().filter(r -> r.getIndex() == round).findFirst());

    return map(
        game,
        gameRound,
        (g, r) -> {
          int totalRoundCount = g.getRounds().size();
          return new GameRound(g.getName(), r.getJsonBlob(), totalRoundCount);
        });
  }
}
