package com.parkinson.benjamin.geogames.service;

import static com.parkinson.benjamin.geogames.helper.OptionalHelper.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkinson.benjamin.geogames.dao.GameData;
import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.dao.GameType;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.GameCreationResponse;
import com.parkinson.benjamin.geogames.model.GameRound;
import com.parkinson.benjamin.geogames.model.GameRoundAnswerRequest;
import com.parkinson.benjamin.geogames.model.River;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GameService {

  private static final int MAX_ROUNDS = 10;
  private static final String RANDOM_GAME_SELECTION = "RANDOM";

  private static final Random random = new Random(System.currentTimeMillis());

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

  public Optional<GameCreationResponse> createGame(String gameType, int numberOfRounds)
      throws IOException {
    if (numberOfRounds < 1 || numberOfRounds > MAX_ROUNDS) {
      throw new IllegalArgumentException(
          "Number of rounds must be between 1 and %d, was %d!"
              .formatted(MAX_ROUNDS, numberOfRounds));
    }

    List<GameRoundEntity> gameRoundEntities = new ArrayList<>(numberOfRounds);

    if (GameType.TRIPOINT.name().equals(gameType)) {
      gameRoundEntities.addAll(createTripointRounds(numberOfRounds));
    }

    if (GameType.RIVERS_BY_SHAPE.name().equals(gameType)) {
      gameRoundEntities.addAll(createRiversByShapeRounds(numberOfRounds));
    }

    if (RANDOM_GAME_SELECTION.equals(gameType)) {
      Map<GameType, Integer> gameTypeCount = new HashMap<>();
      for (int i = 0; i < numberOfRounds; i++) {
        int randomGameType = random.nextInt(GameType.values().length);
        gameTypeCount.put(GameType.values()[randomGameType], gameTypeCount.getOrDefault(GameType.values()[randomGameType], 0) + 1);
      }

      List<GameData> allGameData = new ArrayList<>(numberOfRounds);
      allGameData.addAll(getXRandomTripoints(gameTypeCount.getOrDefault(GameType.TRIPOINT, 0)));
      allGameData.addAll(getXRandomRivers(gameTypeCount.getOrDefault(GameType.RIVERS_BY_SHAPE, 0)));
      Collections.shuffle(allGameData, random);
      gameRoundEntities.addAll(createGameRounds(allGameData));
    }

    if (gameRoundEntities.isEmpty()) {
      throw new IllegalArgumentException("Unknown game type %s!".formatted(gameType));
    }

    GameEntity newGameEntity = new GameEntity(gameRoundEntities);
    GameEntity saved = gameRepository.save(newGameEntity);

    return Optional.of(new GameCreationResponse(saved.getId()));
  }

  @NotNull
  private List<GameRoundEntity> createTripointRounds(int numberOfRounds) throws IOException {
    List<GameData> randomTripoints = getXRandomTripoints(numberOfRounds);

    return createGameRounds(randomTripoints);
  }

  private List<GameData> getXRandomTripoints(int numberOfRounds) throws IOException {
    List<Country> countries = countryLoaderService.loadCountries();
    List<GameData> randomTripoints =
        tripointFinderService.findRandomTripoints(countries, numberOfRounds);
    return randomTripoints;
  }

  @NotNull
  private List<GameRoundEntity> createRiversByShapeRounds(int numberOfRounds) throws IOException {
    List<GameData> randomRivers = getXRandomRivers(numberOfRounds);
    return createGameRounds(randomRivers);
  }

  private List<GameData> getXRandomRivers(int numberOfRounds) throws IOException {
    List<River> rivers = riverLoaderService.loadRivers();
    List<GameData> randomRivers = riverFinderService.findRandomRivers(rivers, numberOfRounds);
    return randomRivers;
  }

  private List<GameRoundEntity> createGameRounds(List<GameData> gameData)
      throws JsonProcessingException {
    List<GameRoundEntity> gameRoundEntities = new ArrayList<>(gameData.size());
    for (int i = 0; i < gameData.size(); i++) {
      GameData gameDatum = gameData.get(i);
      gameRoundEntities.add(
          new GameRoundEntity(i, gameDatum.gameType(), objectMapper.writeValueAsString(gameDatum)));
    }
    return gameRoundEntities;
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
          return new GameRound(r.getGameType(), r.getJsonBlob(), totalRoundCount, r.getAnswerState());
        });
  }

  public Optional<GameRound> submitAnswer(long gameId, int round, AnswerState answerState) {
    Optional<GameEntity> game = gameRepository.findById(gameId);
    Optional<GameRoundEntity> gameRound =
        game.flatMap(g -> g.getRounds().stream().filter(r -> r.getIndex() == round).findFirst());

    return map(
        game,
        gameRound,
        (g, r) -> {
          r.setAnswerState(answerState);
          gameRepository.save(g);
          int totalRoundCount = g.getRounds().size();
          return new GameRound(r.getGameType(), r.getJsonBlob(), totalRoundCount, r.getAnswerState());
        });
  }
}
