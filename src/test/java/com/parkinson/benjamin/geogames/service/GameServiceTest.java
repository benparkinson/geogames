package com.parkinson.benjamin.geogames.service;

import static com.parkinson.benjamin.geogames.TestFixtures.game;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.api.BadRequestException;
import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.api.GameResult;
import com.parkinson.benjamin.geogames.api.GameRound;
import com.parkinson.benjamin.geogames.api.GameTypeResponse;
import com.parkinson.benjamin.geogames.api.RecordedGuess;
import com.parkinson.benjamin.geogames.api.RoundAnswerResponse;
import com.parkinson.benjamin.geogames.api.RoundGuessesResponse;
import com.parkinson.benjamin.geogames.api.RoundHintsResponse;
import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.dao.RoundGuess;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameState;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.LocatedPlaceName;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.RiverRound;
import com.parkinson.benjamin.geogames.model.Scoring;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import com.parkinson.benjamin.geogames.service.challenge.ChallengeConflictException;
import com.parkinson.benjamin.geogames.service.daily.DailyScoreStore;
import com.parkinson.benjamin.geogames.service.daily.PinnedRound;
import com.parkinson.benjamin.geogames.service.metrics.GameMetrics;
import com.parkinson.benjamin.geogames.service.stats.RoundAnswered;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class GameServiceTest {

  private static final UUID GAME_ID = UUID.fromString("0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d");

  private static final UUID OTHER_ID = UUID.fromString("5a1e9d3c-7b24-4f60-8c11-2e3d4f5a6b7c");

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  private GameRepository gameRepository;
  private RoundGenerator tripointGenerator;
  private RoundGenerator riverGenerator;
  private final Map<GameType, RoundGenerator> generators = new EnumMap<>(GameType.class);
  private GameService service;
  private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
  private final DailyScoreStore dailyScores = mock(DailyScoreStore.class);
  private final RoundContinents roundContinents = mock(RoundContinents.class);
  private final KnownPlaceNames names = testNames();

  @BeforeEach
  void setUp() {
    gameRepository = mock(GameRepository.class);
    for (GameType type : GameType.values()) {
      RoundGenerator generator = generatorFor(type);
      when(generator.generate(anyInt(), any(), any()))
          .thenAnswer(invocation -> roundsOf(type, invocation.getArgument(0)));
      generators.put(type, generator);
    }
    tripointGenerator = generators.get(GameType.TRIPOINT);
    riverGenerator = generators.get(GameType.RIVERS_BY_SHAPE);
    service = allGeneratorsService(new Random(1));

    when(gameRepository.save(any(GameEntity.class)))
        .thenAnswer(
            invocation -> {
              GameEntity game = invocation.getArgument(0);
              ReflectionTestUtils.setField(game, "id", 42L);
              return game;
            });
  }

  /** Real lookup over a few made-up names, so a guess resolves as it does in the app. */
  static KnownPlaceNames testNames() {
    CountryNameService countries = mock(CountryNameService.class);
    when(countries.loadCountryNames())
        .thenReturn(
            List.of(
                new LocatedPlaceName("France", List.of("République française"), 0, 0),
                new LocatedPlaceName("Germany", List.of("Deutschland"), 0, 0),
                new LocatedPlaceName("Italy", List.of(), 0, 0)));
    RiverLoaderService rivers = mock(RiverLoaderService.class);
    when(rivers.loadRiverNames())
        .thenReturn(List.of(new LocatedPlaceName("Rhine", List.of("Rhein"), 1.5, 2.5)));
    LakeLoaderService lakes = mock(LakeLoaderService.class);
    when(lakes.loadLakeNames())
        .thenReturn(
            List.of(
                new LocatedPlaceName("Garda", List.of(), 0, 0),
                new LocatedPlaceName("Como", List.of(), 0, 0),
                new LocatedPlaceName("Maggiore", List.of(), 0, 0),
                new LocatedPlaceName("Lugano", List.of(), 0, 0),
                new LocatedPlaceName("Iseo", List.of(), 0, 0),
                new LocatedPlaceName("Orta", List.of(), 0, 0)));
    return new KnownPlaceNames(countries, rivers, lakes);
  }

  // ---------------------------------------------------------------- fixtures

  private static RoundGenerator generatorFor(GameType type) {
    RoundGenerator generator = mock(RoundGenerator.class);
    when(generator.gameType()).thenReturn(type);
    return generator;
  }

  private GameService allGeneratorsService(Random random) {
    return new GameService(
        gameRepository,
        new RoundGenerators(List.copyOf(generators.values())),
        new RoundRules(names),
        objectMapper,
        random,
        events,
        names,
        dailyScores,
        roundContinents,
        mock(GameMetrics.class));
  }

  private List<GameData> roundsOf(GameType type, int count) {
    return type == GameType.TRIPOINT ? tripoints(count) : rivers(count, type);
  }

  private GameService serviceWith(Random random, RoundGenerator... generators) {
    return new GameService(
        gameRepository,
        new RoundGenerators(List.of(generators)),
        new RoundRules(names),
        objectMapper,
        random,
        events,
        names,
        dailyScores,
        roundContinents,
        mock(GameMetrics.class));
  }

  private static GameRoundEntity round(int index, AnswerState state) {
    GameRoundEntity entity = new GameRoundEntity(index, GameType.TRIPOINT, "{}");
    entity.recordAnswer(state, entity.getHintsUsed(), entity.getWrongGuesses(), null);
    return entity;
  }

  private static GameRoundEntity hintedRound(
      int index, GameType type, AnswerState state, int hintsUsed) {
    GameRoundEntity entity = new GameRoundEntity(index, type, "{}");
    entity.recordAnswer(state, hintsUsed, entity.getWrongGuesses(), null);
    return entity;
  }

  private GameCreationResponse createGame(String gameType, int rounds) {
    return service.createGame(gameType, rounds, Difficulty.NORMAL);
  }

  private static Country square(String name) {
    List<List<List<Double>>> ring =
        List.of(
            List.of(List.of(0.0, 0.0), List.of(1.0, 0.0), List.of(1.0, 1.0), List.of(0.0, 0.0)));
    GeoData geoData =
        new GeoData("Feature", new GeoProperties(name), new GeoGeometry("Polygon", ring));
    return new Country(name, List.of("alias of " + name), geoData);
  }

  private static Tripoint tripoint(int n) {
    return new Tripoint(
        new Coordinate(n, n), Set.of(square("A" + n), square("B" + n), square("C" + n)));
  }

  private static RiverRound river(int n, GameType type) {
    int id = type.ordinal() * 100 + n;
    List<List<List<Double>>> lines = List.of(List.of(List.of(0.0, 0.0), List.of(1.0, 1.0)));
    GeoData geoData =
        new GeoData(
            "Feature", new GeoProperties("River" + id), new GeoGeometry("MultiLineString", lines));
    return new RiverRound(
        type,
        new River(
            "Q" + id,
            "River" + id,
            List.of("Alias" + id),
            "Africa",
            geoData,
            new Coordinate(0, 0),
            new Coordinate(1, 1),
            List.of()),
        List.of(),
        null);
  }

  private List<GameData> tripoints(int count) {
    return IntStream.range(0, count).<GameData>mapToObj(GameServiceTest::tripoint).toList();
  }

  private List<GameData> rivers(int count) {
    return rivers(count, GameType.RIVERS_BY_SHAPE);
  }

  private List<GameData> rivers(int count, GameType type) {
    return IntStream.range(0, count).<GameData>mapToObj(n -> river(n, type)).toList();
  }

  private GameEntity savedGame() {
    ArgumentCaptor<GameEntity> captor = ArgumentCaptor.forClass(GameEntity.class);
    verify(gameRepository).save(captor.capture());
    return captor.getValue();
  }

  private JsonNode parse(String json) {
    return objectMapper.readTree(json);
  }

  // -------------------------------------------------------------- createGame

  @ParameterizedTest
  @ValueSource(ints = {0, 11, -1, 100})
  void rejectsRoundCountsOutsideOneToTen(int rounds) {
    assertThatThrownBy(() -> createGame("TRIPOINT", rounds))
        .isInstanceOf(IllegalArgumentException.class);
    verify(gameRepository, never()).save(any());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 10})
  void acceptsBoundaryRoundCounts(int rounds) {
    when(tripointGenerator.generate(eq(rounds), eq(Set.of()), any())).thenReturn(tripoints(rounds));

    assertThat(createGame("TRIPOINT", rounds)).isNotNull();
    assertThat(savedGame().getRounds()).hasSize(rounds);
  }

  @ParameterizedTest
  @ValueSource(strings = {"FOO", "tripoint", ""})
  void rejectsUnknownGameTypes(String type) {
    assertThatThrownBy(() -> createGame(type, 3)).isInstanceOf(IllegalArgumentException.class);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void rejectsNullGameType() {
    assertThatThrownBy(() -> createGame(null, 3)).isInstanceOf(IllegalArgumentException.class);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void createsTripointRounds() {
    when(tripointGenerator.generate(eq(3), eq(Set.of()), any())).thenReturn(tripoints(3));

    GameCreationResponse response = createGame("TRIPOINT", 3);

    assertThat(response).isEqualTo(new GameCreationResponse(savedGame().getPublicId()));
    verify(tripointGenerator, times(1)).generate(eq(3), eq(Set.of()), any());
    verify(riverGenerator, never()).generate(anyInt(), any(), any());

    List<GameRoundEntity> rounds = savedGame().getRounds();
    assertThat(rounds).hasSize(3);
    for (int i = 0; i < 3; i++) {
      GameRoundEntity round = rounds.get(i);
      assertThat(round.getRoundNumber()).isEqualTo(i);
      assertThat(round.getGameType()).isSameAs(GameType.TRIPOINT);
      assertThat(round.getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
      assertThat(round.getRoundKey()).isEqualTo(tripoint(i).roundKey());
    }
  }

  @Test
  void getGameRoundRebuildsTheJsonFromTheStoredKey() {
    Tripoint tripoint = tripoint(1);
    when(tripointGenerator.rebuild(tripoint.roundKey())).thenReturn(tripoint);
    GameRoundEntity entity = new GameRoundEntity(0, GameType.TRIPOINT, tripoint.roundKey());
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game(entity)));

    GameRound result = service.getGameRound(GAME_ID, 0).orElseThrow();

    assertThat(result.jsonBlob()).isEqualTo(objectMapper.writeValueAsString(tripoint));
    JsonNode blob = parse(result.jsonBlob());
    assertThat(blob.get("countries").size()).isEqualTo(3);
    assertThat(blob.has("coordinate")).isTrue();
    assertThat(entity.getRoundKey()).isEqualTo(tripoint.roundKey());
  }

  @Test
  void aDifficultyReachesTheEntityAndTheRoundResponse() {
    when(tripointGenerator.generate(eq(1), eq(Set.of()), any())).thenReturn(tripoints(1));

    service.createGame("TRIPOINT", 1, Difficulty.HARD);

    GameEntity saved = savedGame();
    assertThat(saved.getDifficulty()).isEqualTo(Difficulty.HARD);
    Tripoint tripoint = tripoint(0);
    when(tripointGenerator.rebuild(any())).thenReturn(tripoint);
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(saved));
    assertThat(service.getGameRound(GAME_ID, 0).orElseThrow().difficulty()).isEqualTo("HARD");
  }

  @Test
  void anUnknownDifficultyReadsAsNormalInTheResponseButStaysNullInTheEvent() {
    Tripoint tripoint = tripoint(1);
    when(tripointGenerator.rebuild(tripoint.roundKey())).thenReturn(tripoint);
    GameEntity game = game(new GameRoundEntity(0, GameType.TRIPOINT, tripoint.roundKey()));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    assertThat(service.getGameRound(GAME_ID, 0).orElseThrow().difficulty()).isEqualTo("NORMAL");

    service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, null);
    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(events).publishEvent(captor.capture());
    assertThat(((RoundAnswered) captor.getValue()).hardMode()).isNull();
  }

  private GameRound readRound(Difficulty difficulty, GameType type, String key) {
    GameRoundEntity entity = new GameRoundEntity(0, type, key);
    GameEntity game = new GameEntity(new ArrayList<>(List.of(entity)), type.name(), difficulty);
    when(generators.get(type).rebuild(key)).thenReturn(rivers(1, type).get(0));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));
    return service.getGameRound(GAME_ID, 0).orElseThrow();
  }

  @Test
  void anEasyRoundCarriesTheEffectiveLimitsAndItsContinents() {
    when(roundContinents.of(any())).thenReturn(List.of("Europe", "Asia"));

    GameRound result = readRound(Difficulty.EASY, GameType.LAKES_BY_SHAPE, "lake:Q1");

    assertThat(result.difficulty()).isEqualTo("EASY");
    assertThat(result.continents()).containsExactly("Europe", "Asia");
    assertThat(result.gameType().guessLimit()).isEqualTo(8);
    assertThat(result.gameType().hintCount()).isEqualTo(2);
  }

  @Test
  void aNormalOrUnknownRoundHasNoContinentsAndTheNormalLimits() {
    GameRound normal = readRound(Difficulty.NORMAL, GameType.LAKES_BY_SHAPE, "lake:Q1");
    GameRound unknown = readRound(null, GameType.LAKES_BY_SHAPE, "lake:Q1");

    for (GameRound result : List.of(normal, unknown)) {
      assertThat(result.difficulty()).isEqualTo("NORMAL");
      assertThat(result.continents()).isEmpty();
      assertThat(result.gameType().guessLimit()).isEqualTo(5);
      assertThat(result.gameType().hintCount()).isEqualTo(3);
    }
    verifyNoInteractions(roundContinents);
  }

  @Test
  void aHardRoundIsHard() {
    GameRound result = readRound(Difficulty.HARD, GameType.RIVERS_BY_SHAPE, "river:Q1");

    assertThat(result.difficulty()).isEqualTo("HARD");
  }

  @Test
  void theHardModeOfTheGameIsInTheEvent() {
    GameEntity game =
        new GameEntity(
            new ArrayList<>(List.of(round(0, AnswerState.UNANSWERED))),
            "TRIPOINT",
            Difficulty.HARD);
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, null);

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(events).publishEvent(captor.capture());
    assertThat(((RoundAnswered) captor.getValue()).hardMode()).isTrue();
  }

  @Test
  void submitAnswerDoesNotRebuildTheRound() {
    RiverRound river = (RiverRound) rivers(1).get(0);
    GameRoundEntity entity = new GameRoundEntity(0, GameType.RIVERS_BY_SHAPE, river.roundKey());
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game(entity)));

    RoundAnswerResponse result =
        service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, null).orElseThrow();

    assertThat(result.answerState()).isEqualTo(AnswerState.GAVE_UP);
    verify(riverGenerator, never()).rebuild(any());
  }

  @Test
  void aRoundStoredUnderACountrysOldNameStillLoads() {
    Tripoint tripoint = tripoint(1);
    when(tripointGenerator.rebuild("tripoint:France|Germany|Italy@1.0,2.0")).thenReturn(tripoint);
    GameRoundEntity entity =
        new GameRoundEntity(0, GameType.TRIPOINT, "tripoint:Deutschland|France|Italy@1.0,2.0");
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game(entity)));

    GameRound result = service.getGameRound(GAME_ID, 0).orElseThrow();

    assertThat(result.jsonBlob()).isEqualTo(objectMapper.writeValueAsString(tripoint));
  }

  @Test
  void missingRoundDataReachesTheCaller() {
    when(tripointGenerator.rebuild("tripoint:gone"))
        .thenThrow(new RoundDataMissingException(GameType.TRIPOINT, "tripoint:gone"));
    when(gameRepository.findByPublicId(GAME_ID))
        .thenReturn(Optional.of(game(new GameRoundEntity(0, GameType.TRIPOINT, "tripoint:gone"))));

    assertThatThrownBy(() -> service.getGameRound(GAME_ID, 0))
        .isInstanceOf(RoundDataMissingException.class);
    assertThat(service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, null)).isPresent();
  }

  @Test
  void createsRiverRounds() {
    when(riverGenerator.generate(eq(2), eq(Set.of()), any())).thenReturn(rivers(2));

    createGame("RIVERS_BY_SHAPE", 2);

    verify(riverGenerator).generate(eq(2), eq(Set.of()), any());
    verify(tripointGenerator, never()).generate(anyInt(), any(), any());

    List<GameRoundEntity> rounds = savedGame().getRounds();
    assertThat(rounds).hasSize(2);
    for (int i = 0; i < 2; i++) {
      GameRoundEntity round = rounds.get(i);
      assertThat(round.getRoundNumber()).isEqualTo(i);
      assertThat(round.getGameType()).isSameAs(GameType.RIVERS_BY_SHAPE);
      assertThat(round.getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
      assertThat(round.getRoundKey()).isEqualTo(rivers(2).get(i).roundKey());
    }
  }

  @Test
  void aSingleTypeGameRecordsItsTypeAsTheMode() {
    createGame("RIVERS_BY_SHAPE", 1);

    assertThat(savedGame().getGameMode()).isEqualTo("RIVERS_BY_SHAPE");
  }

  @Test
  void aMixedGameRecordsRandomAsTheMode() {
    createGame("RANDOM", 3);

    assertThat(savedGame().getGameMode()).isEqualTo("RANDOM");
  }

  @RepeatedTest(20)
  void createsRandomRoundsSplitAcrossEveryType() {
    int total = 6;
    service = allGeneratorsService(new Random());

    createGame("RANDOM", total);

    Map<GameType, Integer> requested = new EnumMap<>(GameType.class);
    for (GameType type : GameType.values()) {
      ArgumentCaptor<Integer> count = ArgumentCaptor.forClass(Integer.class);
      verify(generators.get(type), atMost(1)).generate(count.capture(), any(), any());
      requested.put(type, count.getAllValues().stream().mapToInt(Integer::intValue).sum());
    }
    assertThat(requested.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(total);

    List<GameRoundEntity> rounds = savedGame().getRounds();
    assertThat(rounds).hasSize(total);
    assertThat(rounds.stream().map(GameRoundEntity::getRoundNumber).toList())
        .containsExactly(0, 1, 2, 3, 4, 5);
    for (GameType type : GameType.values()) {
      assertThat(rounds.stream().filter(r -> r.getGameType() == type).count())
          .isEqualTo((long) requested.get(type));
    }
  }

  @Test
  void refusesToStartWithoutAGeneratorForEveryGameType() {
    assertThatThrownBy(() -> serviceWith(new Random(1), tripointGenerator))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("RIVERS_BY_SHAPE");
  }

  @Test
  void refusesToStartWithTwoGeneratorsForOneGameType() {
    assertThatThrownBy(
            () ->
                serviceWith(
                    new Random(1),
                    tripointGenerator,
                    generatorFor(GameType.TRIPOINT),
                    riverGenerator))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("TRIPOINT");
  }

  @Test
  void notEnoughDataForARequestedTypeIsABadRequestAndNothingIsSaved() {
    when(tripointGenerator.generate(eq(3), eq(Set.of()), any()))
        .thenThrow(
            new IllegalArgumentException("Can't give you 3 tripoints, I only know about 2!"));

    assertThatThrownBy(() -> createGame("TRIPOINT", 3))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("tripoints");
    verify(gameRepository, never()).save(any());
  }

  @Test
  void aMixedGameWithTheSameSeedGetsTheSameMixOfTypes() {
    List<List<GameType>> mixes = new ArrayList<>();
    for (int run = 0; run < 2; run++) {
      GameRepository repository = mock(GameRepository.class);
      when(repository.save(any(GameEntity.class))).thenAnswer(i -> i.getArgument(0));
      new GameService(
              repository,
              new RoundGenerators(List.copyOf(generators.values())),
              new RoundRules(names),
              objectMapper,
              new Random(7),
              events,
              names,
              dailyScores,
              mock(RoundContinents.class),
              mock(GameMetrics.class))
          .createGame("RANDOM", 10, Difficulty.NORMAL);
      ArgumentCaptor<GameEntity> captor = ArgumentCaptor.forClass(GameEntity.class);
      verify(repository).save(captor.capture());
      mixes.add(captor.getValue().getRounds().stream().map(GameRoundEntity::getGameType).toList());
    }

    assertThat(mixes.get(0)).hasSize(10).isEqualTo(mixes.get(1));
  }

  @Test
  void aMixedGameTellsLaterGeneratorsWhichSubjectsEarlierOnesUsed() {
    // Generators are called in GameType order, and each must be told every earlier subject.
    Set<String> returnedSoFar = new HashSet<>();
    for (GameType type : GameType.values()) {
      when(generators.get(type).generate(anyInt(), any(), any()))
          .thenAnswer(
              invocation -> {
                Set<String> excluded = invocation.getArgument(1);
                assertThat(excluded).isEqualTo(returnedSoFar);
                List<GameData> made = roundsOf(type, invocation.getArgument(0));
                made.forEach(data -> returnedSoFar.add(data.subjectId()));
                return made;
              });
    }
    service = allGeneratorsService(new Random(7));

    createGame("RANDOM", 10);

    assertThat(returnedSoFar).hasSize(10);
  }

  // ------------------------------------------------------------ getGameRound

  @Test
  void looksGamesUpByPublicId() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(game.getPublicId())).thenReturn(Optional.of(game));

    assertThat(service.getGameRound(game.getPublicId(), 0)).isPresent();
    assertThat(service.getGameRound(UUID.randomUUID(), 0)).isEmpty();
    verify(gameRepository, never()).findById(any());
  }

  @Test
  void getGameRoundIsEmptyForUnknownGame() {
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.empty());

    assertThat(service.getGameRound(GAME_ID, 0)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 2, 99})
  void getGameRoundIsEmptyForUnknownRound(int index) {
    when(gameRepository.findByPublicId(GAME_ID))
        .thenReturn(
            Optional.of(game(round(0, AnswerState.UNANSWERED), round(1, AnswerState.CORRECT))));

    assertThat(service.getGameRound(GAME_ID, index)).isEmpty();
  }

  @Test
  void getGameRoundReturnsTheRequestedRound() {
    GameRoundEntity first = new GameRoundEntity(0, GameType.TRIPOINT, "tripoint:first");
    GameRoundEntity second = new GameRoundEntity(1, GameType.RIVERS_BY_SHAPE, "river:second");
    second.recordAnswer(AnswerState.GAVE_UP, second.getHintsUsed(), second.getWrongGuesses(), null);
    when(riverGenerator.rebuild("river:second")).thenReturn(rivers(1).get(0));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game(first, second)));

    GameRound result = service.getGameRound(GAME_ID, 1).orElseThrow();

    assertThat(result.gameType())
        .isEqualTo(GameTypeResponse.from(GameType.RIVERS_BY_SHAPE, Difficulty.NORMAL));
    assertThat(result.jsonBlob()).isEqualTo(objectMapper.writeValueAsString(rivers(1).get(0)));
    assertThat(result.answerState()).isEqualTo(AnswerState.GAVE_UP);
    assertThat(result.totalRoundCount()).isEqualTo(2);
    assertThat(result.gameResult())
        .isEqualTo(
            new GameResult(
                GameState.IN_PROGRESS,
                1,
                0,
                List.of(AnswerState.UNANSWERED, AnswerState.GAVE_UP),
                20,
                0,
                List.of(0, 0),
                List.of(0, 0),
                List.of(false, false),
                0));
    assertThat(result.hintsUsed()).isZero();
    assertThat(result.points()).isZero();
  }

  // ------------------------------------------------------------ submitAnswer

  @Test
  void submitAnswerIsEmptyForUnknownGameOrRound() {
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.empty());
    when(gameRepository.findByPublicId(OTHER_ID))
        .thenReturn(Optional.of(game(round(0, AnswerState.UNANSWERED))));

    assertThat(service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null, null)).isEmpty();
    assertThat(service.submitAnswer(OTHER_ID, 5, AnswerState.CORRECT, 0, null, null)).isEmpty();
    verify(gameRepository, never()).save(any());
  }

  @Test
  void submitAnswerUpdatesOnlyThatRoundAndAdvancesTheGame() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED), round(1, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse afterFirst =
        service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null, null).orElseThrow();

    assertThat(afterFirst.answerState()).isEqualTo(AnswerState.CORRECT);
    assertThat(afterFirst.gameResult().gameState()).isEqualTo(GameState.IN_PROGRESS);
    assertThat(afterFirst.gameResult().score()).isEqualTo(10);
    assertThat(game.getRounds().get(1).getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
    verify(gameRepository, times(1)).save(game);

    RoundAnswerResponse afterSecond =
        service.submitAnswer(GAME_ID, 1, AnswerState.GAVE_UP, 0, null, null).orElseThrow();

    assertThat(afterSecond.gameResult().gameState()).isEqualTo(GameState.FINISHED);
    assertThat(afterSecond.gameResult().score()).isEqualTo(10);
    assertThat(afterSecond.gameResult().roundAnswerStates())
        .containsExactly(AnswerState.CORRECT, AnswerState.GAVE_UP);
    verify(gameRepository, times(2)).save(game);
  }

  @Test
  void theFirstAnswerPublishesOneEventWithPlainValues() {
    GameEntity game =
        game(
            round(0, AnswerState.UNANSWERED),
            hintedRound(1, GameType.RIVERS_BY_SHAPE, AnswerState.UNANSWERED, 0));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    service.submitAnswer(GAME_ID, 1, AnswerState.CORRECT, 1, 2, null);

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(events).publishEvent(captor.capture());
    assertThat(captor.getValue())
        .isEqualTo(
            new RoundAnswered(
                game.getPlayId(),
                "TRIPOINT",
                2,
                1,
                GameType.RIVERS_BY_SHAPE,
                "{}",
                AnswerState.CORRECT,
                1,
                2,
                1,
                LocalDate.now(ZoneOffset.UTC),
                null,
                null));
  }

  @Test
  void anIdenticalRepeatPublishesNothing() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));
    service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, null);

    service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, null);

    verify(events, times(1)).publishEvent(any(Object.class));
  }

  @Test
  void aConflictingAnswerPublishesNothing() {
    GameEntity game = game(round(0, AnswerState.CORRECT));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, null))
        .isInstanceOf(RoundAlreadyAnsweredException.class);

    verify(events, never()).publishEvent(any(Object.class));
  }

  @Test
  void submitAnswerRefusesToChangeAnAnsweredRound() {
    GameEntity game = game(round(0, AnswerState.CORRECT));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, null))
        .isInstanceOf(RoundAlreadyAnsweredException.class)
        .hasMessageContaining("CORRECT")
        .hasMessageContaining("GAVE_UP");

    assertThat(game.getRounds().get(0).getAnswerState()).isEqualTo(AnswerState.CORRECT);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void submittingTheSameAnswerAgainChangesNothing() {
    GameEntity game = game(round(0, AnswerState.GAVE_UP));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse again =
        service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, null).orElseThrow();

    assertThat(again.answerState()).isEqualTo(AnswerState.GAVE_UP);
    assertThat(again.gameResult().gameState()).isEqualTo(GameState.FINISHED);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void submitAnswerRejectsUnansweredAndLeavesTheRoundAlone() {
    GameEntity game = game(round(0, AnswerState.CORRECT));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    assertThatThrownBy(
            () -> service.submitAnswer(GAME_ID, 0, AnswerState.UNANSWERED, 0, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("UNANSWERED");

    assertThat(game.getRounds().get(0).getAnswerState()).isEqualTo(AnswerState.CORRECT);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void submitAnswerStoresHintsUsed() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse result =
        service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 1, null, null).orElseThrow();

    assertThat(game.getRounds().get(0).getHintsUsed()).isEqualTo(1);
    assertThat(result.hintsUsed()).isEqualTo(1);
    assertThat(result.points()).isEqualTo(9);
    assertThat(result.gameResult().score()).isEqualTo(9);
  }

  @Test
  void submitAnswerRejectsMoreHintsThanTheTypeHas() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 4, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("3 hints")
        .hasMessageContaining("4");

    assertThat(game.getRounds().get(0).getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
    assertThat(game.getRounds().get(0).getHintsUsed()).isZero();
    verify(gameRepository, never()).save(any());
  }

  @Test
  void submitAnswerRejectsNegativeHints() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, -1, null, null))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(game.getRounds().get(0).getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void sameAnswerWithDifferentHintsIsAConflict() {
    GameEntity game = game(hintedRound(0, GameType.TRIPOINT, AnswerState.CORRECT, 1));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null, null))
        .isInstanceOf(RoundAlreadyAnsweredException.class)
        .hasMessageContaining("1 hints")
        .hasMessageContaining("0 hints");

    assertThat(game.getRounds().get(0).getHintsUsed()).isEqualTo(1);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void sameAnswerWithSameHintsIsANoOp() {
    GameEntity game = game(hintedRound(0, GameType.TRIPOINT, AnswerState.CORRECT, 2));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse again =
        service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 2, null, null).orElseThrow();

    assertThat(again.points()).isEqualTo(8);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void revealedRoundScoresZeroWhateverTheHints() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse result =
        service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 3, null, null).orElseThrow();

    assertThat(result.hintsUsed()).isEqualTo(3);
    assertThat(result.points()).isZero();
    assertThat(result.gameResult().score()).isZero();
  }

  @Test
  void outOfGuessesIsStoredScoresNothingAndIsNotCorrect() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse result =
        service.submitAnswer(GAME_ID, 0, AnswerState.OUT_OF_GUESSES, 1, null, null).orElseThrow();

    assertThat(game.getRounds().get(0).getAnswerState()).isEqualTo(AnswerState.OUT_OF_GUESSES);
    assertThat(result.answerState()).isEqualTo(AnswerState.OUT_OF_GUESSES);
    assertThat(result.points()).isZero();
    assertThat(result.gameResult().score()).isZero();
    assertThat(result.gameResult().roundsCorrect()).isZero();
    assertThat(result.gameResult().gameState()).isEqualTo(GameState.FINISHED);
  }

  @Test
  void submittingOutOfGuessesAgainChangesNothing() {
    GameEntity game = game(round(0, AnswerState.OUT_OF_GUESSES));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse again =
        service.submitAnswer(GAME_ID, 0, AnswerState.OUT_OF_GUESSES, 0, null, null).orElseThrow();

    assertThat(again.answerState()).isEqualTo(AnswerState.OUT_OF_GUESSES);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void correctAfterOutOfGuessesIsRefused() {
    GameEntity game = game(round(0, AnswerState.OUT_OF_GUESSES));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null, null))
        .isInstanceOf(RoundAlreadyAnsweredException.class)
        .hasMessageContaining("OUT_OF_GUESSES");

    assertThat(game.getRounds().get(0).getAnswerState()).isEqualTo(AnswerState.OUT_OF_GUESSES);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void aGameCannotBeMadeWithoutRounds() {
    assertThatThrownBy(() -> game())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least one round");
    assertThatThrownBy(() -> new GameEntity(null, "TRIPOINT", null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aGameCannotBeMadeWithoutAMode() {
    assertThatThrownBy(
            () ->
                new GameEntity(
                    new ArrayList<>(List.of(round(0, AnswerState.UNANSWERED))), null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mode");
  }

  // ------------------------------------------------------- daily scores

  private static final LocalDate DAILY_DATE = LocalDate.of(2026, 11, 20);

  private GameEntity dailyGame(GameRoundEntity... rounds) {
    GameEntity game = new GameEntity(new ArrayList<>(List.of(rounds)), "DAILY", null, DAILY_DATE);
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));
    return game;
  }

  @Test
  void aDailyGameIsCountedOnTheAnswerThatFinishesIt() {
    dailyGame(round(0, AnswerState.CORRECT), round(1, AnswerState.UNANSWERED));

    service.submitAnswer(GAME_ID, 1, AnswerState.GAVE_UP, 0, null, null);

    verify(dailyScores).recordFinished(DAILY_DATE, Difficulty.NORMAL, 10, 20);
  }

  @Test
  void repeatingTheFinalAnswerDoesNotCountAgain() {
    dailyGame(round(0, AnswerState.CORRECT), round(1, AnswerState.UNANSWERED));

    service.submitAnswer(GAME_ID, 1, AnswerState.GAVE_UP, 0, null, null);
    service.submitAnswer(GAME_ID, 1, AnswerState.GAVE_UP, 0, null, null);

    verify(dailyScores, times(1)).recordFinished(any(), any(), anyInt(), anyInt());
  }

  @Test
  void anUnfinishedDailyGameIsNotCounted() {
    dailyGame(round(0, AnswerState.UNANSWERED), round(1, AnswerState.UNANSWERED));

    service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null, null);

    verify(dailyScores, never()).recordFinished(any(), any(), anyInt(), anyInt());
  }

  @Test
  void aGameThatIsNotDailyIsNeverCounted() {
    when(gameRepository.findByPublicId(GAME_ID))
        .thenReturn(Optional.of(game(round(0, AnswerState.UNANSWERED))));

    service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null, null);

    verify(dailyScores, never()).recordFinished(any(), any(), anyInt(), anyInt());
  }

  @Test
  void aDailyGameFinishedByGivingUpOrRunningOutOfGuessesIsCounted() {
    dailyGame(round(0, AnswerState.UNANSWERED), round(1, AnswerState.UNANSWERED));
    int limit = GameType.TRIPOINT.guessLimit();

    service.submitAnswer(GAME_ID, 0, AnswerState.OUT_OF_GUESSES, 0, limit, null);
    service.submitAnswer(GAME_ID, 1, AnswerState.GAVE_UP, 0, null, null);

    verify(dailyScores).recordFinished(DAILY_DATE, Difficulty.NORMAL, 0, 20);
  }

  // ------------------------------------------------------------ recordGuess

  private GameRoundEntity guessRound(GameType type, AnswerState state) {
    GameRoundEntity entity = new GameRoundEntity(0, type, "{}");
    entity.recordAnswer(state, entity.getHintsUsed(), entity.getWrongGuesses(), null);
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game(entity)));
    return entity;
  }

  private static List<String> texts(RoundGuessesResponse response) {
    return response.guesses().stream().map(RecordedGuess::text).toList();
  }

  @Test
  void guessesAreStoredInOrderWithTheListSpelling() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);

    service.recordGuess(GAME_ID, 0, "  GERMANY ", false);
    RoundGuessesResponse response =
        service.recordGuess(GAME_ID, 0, "republique FRANCAISE", true).orElseThrow();

    assertThat(response.guesses())
        .containsExactly(
            new RecordedGuess("Germany", false), new RecordedGuess("République française", true));
    assertThat(entity.getGuesses()).hasSize(2);
    verify(gameRepository, times(2)).save(any(GameEntity.class));
  }

  @Test
  void aDuplicateGuessChangesNothing() {
    guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);
    service.recordGuess(GAME_ID, 0, "Germany", false);

    RoundGuessesResponse again = service.recordGuess(GAME_ID, 0, "germany", false).orElseThrow();

    assertThat(texts(again)).containsExactly("Germany");
    verify(gameRepository, times(1)).save(any(GameEntity.class));
  }

  @Test
  void anUnknownGameOrRoundIsEmpty() {
    when(gameRepository.findByPublicId(OTHER_ID)).thenReturn(Optional.empty());
    guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);

    assertThat(service.recordGuess(OTHER_ID, 0, "France", false)).isEmpty();
    assertThat(service.recordGuess(GAME_ID, 5, "France", false)).isEmpty();
  }

  @Test
  void getGameRoundReturnsTheStoredGuesses() {
    Tripoint tripoint = tripoint(1);
    when(tripointGenerator.rebuild(tripoint.roundKey())).thenReturn(tripoint);
    GameRoundEntity entity = new GameRoundEntity(0, GameType.TRIPOINT, tripoint.roundKey());
    entity.addGuess(new RoundGuess("Germany", false));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game(entity)));

    GameRound result = service.getGameRound(GAME_ID, 0).orElseThrow();

    assertThat(result.guesses()).containsExactly(new RecordedGuess("Germany", false));
  }

  // ------------------------------------------------------------- recordHint

  @Test
  void aRepeatedHintChangesNothingAndTheFirstCountWins() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);
    service.recordHint(GAME_ID, 0, 0, 0);
    service.recordGuess(GAME_ID, 0, "France", false);

    RoundHintsResponse again = service.recordHint(GAME_ID, 0, 0, 1).orElseThrow();

    assertThat(again.hintsTakenAt()).containsExactly(0);
    assertThat(entity.getHintsTakenAt()).containsExactly(0);
    verify(gameRepository, times(2)).save(any(GameEntity.class));
  }

  @Test
  void aHintForAnUnknownGameOrRoundIsEmpty() {
    when(gameRepository.findByPublicId(OTHER_ID)).thenReturn(Optional.empty());
    guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);

    assertThat(service.recordHint(OTHER_ID, 0, 0, 0)).isEmpty();
    assertThat(service.recordHint(GAME_ID, 5, 0, 0)).isEmpty();
  }

  @Test
  void getGameRoundReturnsTheHintsTaken() {
    Tripoint tripoint = tripoint(1);
    when(tripointGenerator.rebuild(tripoint.roundKey())).thenReturn(tripoint);
    GameRoundEntity entity = new GameRoundEntity(0, GameType.TRIPOINT, tripoint.roundKey());
    entity.addHint(0);
    entity.addHint(2);
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game(entity)));

    GameRound result = service.getGameRound(GAME_ID, 0).orElseThrow();

    assertThat(result.hintsTakenAt()).containsExactly(0, 2);
    assertThat(result.hintsUsed()).isZero();
  }

  @Test
  void anAnswerReportingFewerHintsThanRecordedIsChargedForTheRecordedOnes() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);
    entity.addHint(0);
    entity.addHint(0);

    RoundAnswerResponse result =
        service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null, null).orElseThrow();

    assertThat(entity.getHintsUsed()).isEqualTo(2);
    assertThat(result.hintsUsed()).isEqualTo(2);
    assertThat(result.points())
        .isEqualTo(
            Scoring.roundPoints(AnswerState.CORRECT, 2, 3, GameType.TRIPOINT.answerCount(), 0));
    ArgumentCaptor<RoundAnswered> event = ArgumentCaptor.forClass(RoundAnswered.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().hintsUsed()).isEqualTo(2);
  }

  @Test
  void aRepeatOfThatAnswerIsANoOp() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);
    entity.addHint(0);
    service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null, null);

    RoundAnswerResponse again =
        service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null, null).orElseThrow();

    assertThat(again.hintsUsed()).isEqualTo(1);
    verify(events, times(1)).publishEvent(any(Object.class));
  }

  @Test
  void anAnswerReportingMoreHintsThanRecordedIsAccepted() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);
    entity.addHint(0);

    service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 2, null, null);

    assertThat(entity.getHintsUsed()).isEqualTo(2);
  }

  @Test
  void aPartlyAnsweredTripointScoresItsShare() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse result =
        service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, 2).orElseThrow();

    assertThat(result.points()).isEqualTo(5);
    assertThat(game.getRounds().get(0).getAnswersFound()).isEqualTo(2);
    ArgumentCaptor<RoundAnswered> event = ArgumentCaptor.forClass(RoundAnswered.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().answersFound()).isEqualTo(2);
  }

  @Test
  void recordedCorrectGuessesCountWhenTheClientDoesNotReportAnyFound() {
    GameRoundEntity unanswered = round(0, AnswerState.UNANSWERED);
    unanswered.addGuess(new RoundGuess("A", true));
    unanswered.addGuess(new RoundGuess("B", true));
    unanswered.addGuess(new RoundGuess("X", false));
    GameEntity game = game(unanswered);
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse result =
        service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, null).orElseThrow();

    assertThat(result.points()).isEqualTo(5);
    assertThat(unanswered.getAnswersFound()).isEqualTo(2);
  }

  @Test
  void theLargerOfReportedAndRecordedAnswersFoundCounts() {
    GameRoundEntity unanswered = round(0, AnswerState.UNANSWERED);
    unanswered.addGuess(new RoundGuess("A", true));
    GameEntity game = game(unanswered);
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse result =
        service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, 2).orElseThrow();

    assertThat(result.points()).isEqualTo(5);
  }

  @Test
  void aRevealedRoundWithNothingReportedOrRecordedStoresNoAnswersFound() {
    GameRoundEntity unanswered = round(0, AnswerState.UNANSWERED);
    GameEntity game = game(unanswered);
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse result =
        service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, null).orElseThrow();

    assertThat(result.points()).isZero();
    assertThat(unanswered.getAnswersFound()).isNull();
  }

  @Test
  void aCorrectRoundStoresEveryAnswerFound() {
    GameRoundEntity unanswered = round(0, AnswerState.UNANSWERED);
    GameEntity game = game(unanswered);
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse result =
        service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null, null).orElseThrow();

    assertThat(result.points()).isEqualTo(10);
    assertThat(unanswered.getAnswersFound()).isEqualTo(3);
  }

  @Test
  void aRepeatedPartialAnswerIsANoOpAndAChangedOneIsAConflict() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));
    service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, 2);

    assertThat(service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, 2)).isPresent();
    verify(gameRepository, times(1)).save(any());
    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, 1))
        .isInstanceOf(RoundAlreadyAnsweredException.class);
  }

  @Test
  void anImpossibleAnswersFoundIsRejected() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null, 3))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(game.getRounds().get(0).getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
  }

  // ---------------------------------------------------------------- challenge source

  private void storedGame(LocalDate dailyDate, AnswerState second) {
    GameRoundEntity r0 = new GameRoundEntity(0, GameType.TRIPOINT, "tripoint:a");
    GameRoundEntity r1 = new GameRoundEntity(1, GameType.LAKES_BY_SHAPE, "lake:Q1");
    r0.recordAnswer(AnswerState.CORRECT, 0, 0, 1);
    r1.recordAnswer(second, 0, 0, null);
    GameEntity game = new GameEntity(new ArrayList<>(List.of(r1, r0)), "RANDOM", null, dailyDate);
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));
  }

  @Test
  void aFinishedGameBecomesAChallengeSourceWithItsRoundsInOrder() {
    storedGame(null, AnswerState.GAVE_UP);

    ChallengeSource source = service.challengeSource(GAME_ID).orElseThrow();

    assertThat(source.rounds())
        .containsExactly(
            new PinnedRound(GameType.TRIPOINT, "tripoint:a"),
            new PinnedRound(GameType.LAKES_BY_SHAPE, "lake:Q1"));
    assertThat(source.gameMode()).isEqualTo("RANDOM");
    assertThat(source.difficulty()).isEqualTo(Difficulty.NORMAL);
    assertThat(source.score()).isEqualTo(10);
    assertThat(source.maxScore()).isEqualTo(20);
  }

  @Test
  void anUnfinishedGameCannotBeChallenged() {
    storedGame(null, AnswerState.UNANSWERED);

    assertThatThrownBy(() -> service.challengeSource(GAME_ID))
        .isInstanceOf(ChallengeConflictException.class);
  }

  @Test
  void aDailyGameCannotBeChallenged() {
    storedGame(LocalDate.of(2026, 11, 20), AnswerState.CORRECT);

    assertThatThrownBy(() -> service.challengeSource(GAME_ID))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void anUnknownGameHasNoChallengeSource() {
    when(gameRepository.findByPublicId(OTHER_ID)).thenReturn(Optional.empty());

    assertThat(service.challengeSource(OTHER_ID)).isEmpty();
  }
}
