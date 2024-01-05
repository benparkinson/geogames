package com.parkinson.benjamin.geogames.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkinson.benjamin.geogames.dao.GameData;
import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.dao.GameType;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Game;
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

  private final GameRepository gameRepository;
  private final CountryLoaderService countryLoaderService;
  private final TripointFinderService tripointFinderService;
  private final RiverLoaderService riverLoaderService;
  private final RiverFinderService riverFinderService;
  private final ObjectMapper objectMapper;

  @Autowired
  public GameService(GameRepository gameRepository, CountryLoaderService countryLoaderService,
      TripointFinderService tripointFinderService, RiverLoaderService riverLoaderService,
      RiverFinderService riverFinderService, ObjectMapper objectMapper) {
    this.gameRepository = gameRepository;
    this.countryLoaderService = countryLoaderService;
    this.tripointFinderService = tripointFinderService;
    this.riverLoaderService = riverLoaderService;
    this.riverFinderService = riverFinderService;
    this.objectMapper = objectMapper;
  }

  public Optional<GameCreationResponse> createGame(GameType gameType) throws IOException {

    if (gameType == GameType.TRIPOINT) {

      List<Country> countries = countryLoaderService.loadCountries();
      List<GameData> randomTripoints = tripointFinderService.findRandomTripoints(countries, 5);

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
      List<River> randomRivers = riverFinderService.findRandomRivers(rivers, 5);
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

  public Optional<Game> getGameById(long gameId, Optional<Integer> round) {
    Optional<GameEntity> game = gameRepository.findById(gameId);
    Optional<GameEntity> gameWithRound = game.map(g -> new GameEntity(g.getName(),
        g.getRounds().stream().filter(r -> r.getIndex() == round.orElse(0)).toList()));

    return gameWithRound.map(this::convertEntity);
  }

  private Game convertEntity(GameEntity gameEntity) {
    var gameRounds = gameEntity.getRounds().stream().map(roundEntity ->
        new GameRound(roundEntity.getJsonBlob())).toList();

    return new Game(gameRounds);
  }

}
