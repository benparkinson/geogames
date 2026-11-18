package com.parkinson.benjamin.geogames.service;

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
import static org.mockito.Mockito.when;

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
import com.parkinson.benjamin.geogames.model.CountryName;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameState;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.PlaceName;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.RiverRound;
import com.parkinson.benjamin.geogames.model.Scoring;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
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
              game.setId(42L);
              return game;
            });
  }

  /** Real lookup over a few made-up names, so a guess resolves as it does in the app. */
  static KnownPlaceNames testNames() {
    CountryLoaderService countries = mock(CountryLoaderService.class);
    when(countries.loadCountryNames())
        .thenReturn(
            List.of(
                new CountryName("France", List.of("République française"), 0, 0),
                new CountryName("Germany", List.of("Deutschland"), 0, 0),
                new CountryName("Italy", List.of(), 0, 0)));
    RiverLoaderService rivers = mock(RiverLoaderService.class);
    when(rivers.loadRiverNames()).thenReturn(List.of(new PlaceName("Rhine", List.of("Rhein"))));
    LakeLoaderService lakes = mock(LakeLoaderService.class);
    when(lakes.loadLakeNames())
        .thenReturn(
            List.of(
                new PlaceName("Garda", List.of()),
                new PlaceName("Como", List.of()),
                new PlaceName("Maggiore", List.of()),
                new PlaceName("Lugano", List.of()),
                new PlaceName("Iseo", List.of()),
                new PlaceName("Orta", List.of())));
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
        gameRepository, List.copyOf(generators.values()), objectMapper, random, events, names);
  }

  private List<GameData> roundsOf(GameType type, int count) {
    return type == GameType.TRIPOINT ? tripoints(count) : rivers(count, type);
  }

  private GameService serviceWith(Random random, RoundGenerator... generators) {
    return new GameService(
        gameRepository, List.of(generators), objectMapper, random, events, names);
  }

  private static GameRoundEntity round(int index, AnswerState state) {
    GameRoundEntity entity = new GameRoundEntity(index, GameType.TRIPOINT, "{}");
    entity.setAnswerState(state);
    return entity;
  }

  private static GameRoundEntity hintedRound(
      int index, GameType type, AnswerState state, int hintsUsed) {
    GameRoundEntity entity = new GameRoundEntity(index, type, "{}");
    entity.setAnswerState(state);
    entity.setHintsUsed(hintsUsed);
    return entity;
  }

  private static GameEntity game(GameRoundEntity... rounds) {
    return new GameEntity(new ArrayList<>(List.of(rounds)), "TRIPOINT");
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
        List.of());
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
    assertThatThrownBy(() -> service.createGame("TRIPOINT", rounds))
        .isInstanceOf(IllegalArgumentException.class);
    verify(gameRepository, never()).save(any());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 10})
  void acceptsBoundaryRoundCounts(int rounds) {
    when(tripointGenerator.generate(eq(rounds), eq(Set.of()), any())).thenReturn(tripoints(rounds));

    assertThat(service.createGame("TRIPOINT", rounds)).isNotNull();
    assertThat(savedGame().getRounds()).hasSize(rounds);
  }

  @ParameterizedTest
  @ValueSource(strings = {"FOO", "tripoint", ""})
  void rejectsUnknownGameTypes(String type) {
    assertThatThrownBy(() -> service.createGame(type, 3))
        .isInstanceOf(IllegalArgumentException.class);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void rejectsNullGameType() {
    assertThatThrownBy(() -> service.createGame(null, 3))
        .isInstanceOf(IllegalArgumentException.class);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void createsTripointRounds() {
    when(tripointGenerator.generate(eq(3), eq(Set.of()), any())).thenReturn(tripoints(3));

    GameCreationResponse response = service.createGame("TRIPOINT", 3);

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
  void submitAnswerDoesNotRebuildTheRound() {
    RiverRound river = (RiverRound) rivers(1).get(0);
    GameRoundEntity entity = new GameRoundEntity(0, GameType.RIVERS_BY_SHAPE, river.roundKey());
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game(entity)));

    RoundAnswerResponse result =
        service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null).orElseThrow();

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
    assertThat(service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null)).isPresent();
  }

  @Test
  void createsRiverRounds() {
    when(riverGenerator.generate(eq(2), eq(Set.of()), any())).thenReturn(rivers(2));

    service.createGame("RIVERS_BY_SHAPE", 2);

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
    service.createGame("RIVERS_BY_SHAPE", 1);

    assertThat(savedGame().getGameMode()).isEqualTo("RIVERS_BY_SHAPE");
  }

  @Test
  void aMixedGameRecordsRandomAsTheMode() {
    service.createGame("RANDOM", 3);

    assertThat(savedGame().getGameMode()).isEqualTo("RANDOM");
  }

  @RepeatedTest(20)
  void createsRandomRoundsSplitAcrossEveryType() {
    int total = 6;
    service = allGeneratorsService(new Random());

    service.createGame("RANDOM", total);

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

    assertThatThrownBy(() -> service.createGame("TRIPOINT", 3))
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
              List.copyOf(generators.values()),
              objectMapper,
              new Random(7),
              events,
              names)
          .createGame("RANDOM", 10);
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

    service.createGame("RANDOM", 10);

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
    second.setAnswerState(AnswerState.GAVE_UP);
    when(riverGenerator.rebuild("river:second")).thenReturn(rivers(1).get(0));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game(first, second)));

    GameRound result = service.getGameRound(GAME_ID, 1).orElseThrow();

    assertThat(result.gameType()).isEqualTo(GameTypeResponse.from(GameType.RIVERS_BY_SHAPE));
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
                200,
                0,
                0,
                List.of(0, 0),
                List.of(0, 0)));
    assertThat(result.hintsUsed()).isZero();
    assertThat(result.points()).isZero();
  }

  // ------------------------------------------------------------ submitAnswer

  @Test
  void submitAnswerIsEmptyForUnknownGameOrRound() {
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.empty());
    when(gameRepository.findByPublicId(OTHER_ID))
        .thenReturn(Optional.of(game(round(0, AnswerState.UNANSWERED))));

    assertThat(service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null)).isEmpty();
    assertThat(service.submitAnswer(OTHER_ID, 5, AnswerState.CORRECT, 0, null)).isEmpty();
    verify(gameRepository, never()).save(any());
  }

  @Test
  void submitAnswerUpdatesOnlyThatRoundAndAdvancesTheGame() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED), round(1, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse afterFirst =
        service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null).orElseThrow();

    assertThat(afterFirst.answerState()).isEqualTo(AnswerState.CORRECT);
    assertThat(afterFirst.gameResult().gameState()).isEqualTo(GameState.IN_PROGRESS);
    assertThat(afterFirst.gameResult().score()).isEqualTo(100);
    assertThat(game.getRounds().get(1).getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
    verify(gameRepository, times(1)).save(game);

    RoundAnswerResponse afterSecond =
        service.submitAnswer(GAME_ID, 1, AnswerState.GAVE_UP, 0, null).orElseThrow();

    assertThat(afterSecond.gameResult().gameState()).isEqualTo(GameState.FINISHED);
    assertThat(afterSecond.gameResult().score()).isEqualTo(100);
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

    service.submitAnswer(GAME_ID, 1, AnswerState.CORRECT, 1, 2);

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
                LocalDate.now(ZoneOffset.UTC)));
  }

  @Test
  void anIdenticalRepeatPublishesNothing() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));
    service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null);

    service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null);

    verify(events, times(1)).publishEvent(any(Object.class));
  }

  @Test
  void aConflictingAnswerPublishesNothing() {
    GameEntity game = game(round(0, AnswerState.CORRECT));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null))
        .isInstanceOf(RoundAlreadyAnsweredException.class);

    verify(events, never()).publishEvent(any(Object.class));
  }

  @Test
  void aGameWithoutAPlayIdPublishesNothing() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    ReflectionTestUtils.setField(game, "playId", null);
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null);

    verify(events, never()).publishEvent(any(Object.class));
  }

  @Test
  void submitAnswerRefusesToChangeAnAnsweredRound() {
    GameEntity game = game(round(0, AnswerState.CORRECT));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null))
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
        service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null).orElseThrow();

    assertThat(again.answerState()).isEqualTo(AnswerState.GAVE_UP);
    assertThat(again.gameResult().gameState()).isEqualTo(GameState.FINISHED);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void submitAnswerRejectsUnansweredAndLeavesTheRoundAlone() {
    GameEntity game = game(round(0, AnswerState.CORRECT));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.UNANSWERED, 0, null))
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
        service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 1, null).orElseThrow();

    assertThat(game.getRounds().get(0).getHintsUsed()).isEqualTo(1);
    assertThat(result.hintsUsed()).isEqualTo(1);
    assertThat(result.points()).isEqualTo(83);
    assertThat(result.gameResult().score()).isEqualTo(83);
  }

  @Test
  void submitAnswerRejectsMoreHintsThanTheTypeHas() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 4, null))
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

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, -1, null))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(game.getRounds().get(0).getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void sameAnswerWithDifferentHintsIsAConflict() {
    GameEntity game = game(hintedRound(0, GameType.TRIPOINT, AnswerState.CORRECT, 1));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null))
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
        service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 2, null).orElseThrow();

    assertThat(again.points()).isEqualTo(67);
    verify(gameRepository, never()).save(any());
  }

  // ------------------------------------------------------ wrong guesses

  /** Submits to a fresh TRIPOINT round (guess limit 5) and returns it. */
  private GameRoundEntity submitToFreshRound(AnswerState state, Integer wrongGuesses) {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));
    service.submitAnswer(GAME_ID, 0, state, 0, wrongGuesses);
    return game.getRounds().get(0);
  }

  private GameRoundEntity expectRejected(AnswerState state, Integer wrongGuesses, String message) {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));
    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, state, 0, wrongGuesses))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(message);
    GameRoundEntity stored = game.getRounds().get(0);
    assertThat(stored.getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
    assertThat(stored.getWrongGuesses()).isNull();
    verify(gameRepository, never()).save(any());
    return stored;
  }

  @Test
  void submitAnswerStoresWrongGuesses() {
    assertThat(submitToFreshRound(AnswerState.CORRECT, 3).getWrongGuesses()).isEqualTo(3);
  }

  @Test
  void aMissingWrongGuessesIsStoredAsUnknownNotZero() {
    assertThat(submitToFreshRound(AnswerState.CORRECT, null).getWrongGuesses()).isNull();
    assertThat(submitToFreshRound(AnswerState.GAVE_UP, null).getWrongGuesses()).isNull();
    assertThat(submitToFreshRound(AnswerState.OUT_OF_GUESSES, null).getWrongGuesses()).isNull();
  }

  @Test
  void correctAndGaveUpAcceptZeroUpToOneBelowTheLimit() {
    assertThat(submitToFreshRound(AnswerState.CORRECT, 0).getWrongGuesses()).isZero();
    assertThat(submitToFreshRound(AnswerState.CORRECT, 4).getWrongGuesses()).isEqualTo(4);
    assertThat(submitToFreshRound(AnswerState.GAVE_UP, 0).getWrongGuesses()).isZero();
    assertThat(submitToFreshRound(AnswerState.GAVE_UP, 4).getWrongGuesses()).isEqualTo(4);
  }

  @Test
  void outOfGuessesAcceptsExactlyTheLimit() {
    assertThat(submitToFreshRound(AnswerState.OUT_OF_GUESSES, 5).getWrongGuesses()).isEqualTo(5);
  }

  @Test
  void negativeWrongGuessesAreRejected() {
    expectRejected(AnswerState.CORRECT, -1, "-1");
  }

  @Test
  void moreWrongGuessesThanTheLimitAreRejected() {
    expectRejected(AnswerState.CORRECT, 6, "allows 5");
    expectRejected(AnswerState.OUT_OF_GUESSES, 6, "allows 5");
  }

  @Test
  void outOfGuessesWithFewerThanTheLimitIsRejected() {
    expectRejected(
        AnswerState.OUT_OF_GUESSES,
        3,
        "A round of TRIPOINT that ran out of guesses had 5 wrong guesses, not 3");
  }

  @Test
  void correctOrGaveUpWithTheLimitIsRejected() {
    expectRejected(AnswerState.CORRECT, 5, "ran out of guesses");
    expectRejected(AnswerState.GAVE_UP, 5, "ran out of guesses");
  }

  @Test
  void sameAnswerWithSameWrongGuessesIsANoOp() {
    GameRoundEntity answered = round(0, AnswerState.CORRECT);
    answered.setWrongGuesses(2);
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game(answered)));

    service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, 2).orElseThrow();

    verify(gameRepository, never()).save(any());
  }

  @Test
  void sameAnswerWithDifferentWrongGuessesIsAConflict() {
    GameRoundEntity answered = round(0, AnswerState.CORRECT);
    answered.setWrongGuesses(2);
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game(answered)));

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, 3))
        .isInstanceOf(RoundAlreadyAnsweredException.class)
        .hasMessageContaining("2 wrong guesses")
        .hasMessageContaining("3 wrong guesses");

    assertThat(answered.getWrongGuesses()).isEqualTo(2);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void unreportedWrongGuessesOnlyMatchUnreported() {
    GameRoundEntity unreported = round(0, AnswerState.CORRECT);
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game(unreported)));

    service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null).orElseThrow();
    verify(gameRepository, never()).save(any());

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, 0))
        .isInstanceOf(RoundAlreadyAnsweredException.class)
        .hasMessageContaining("unreported");
  }

  // ------------------------------------------------------ calculateGameResult

  @Test
  void roundAnswerStatesAreOrderedByIndexAndScored() {
    GameEntity game =
        game(
            round(2, AnswerState.UNANSWERED),
            round(0, AnswerState.CORRECT),
            round(1, AnswerState.GAVE_UP));

    GameResult result = GameService.calculateGameResult(game);

    assertThat(result.roundAnswerStates())
        .containsExactly(AnswerState.CORRECT, AnswerState.GAVE_UP, AnswerState.UNANSWERED);
    assertThat(result.score()).isEqualTo(100);
    assertThat(result.roundsUnanswered()).isEqualTo(1);
    assertThat(result.gameState()).isEqualTo(GameState.IN_PROGRESS);
  }

  @Test
  void gameIsFinishedWhenEveryRoundIsAnswered() {
    GameEntity game = game(round(1, AnswerState.GAVE_UP), round(0, AnswerState.CORRECT));

    GameResult result = GameService.calculateGameResult(game);

    assertThat(result.gameState()).isEqualTo(GameState.FINISHED);
    assertThat(result.roundsUnanswered()).isZero();
    assertThat(result.roundAnswerStates())
        .containsExactly(AnswerState.CORRECT, AnswerState.GAVE_UP);
  }

  @Test
  void gameIsCreatedWhenNothingIsAnswered() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED), round(1, AnswerState.UNANSWERED));

    GameResult result = GameService.calculateGameResult(game);

    assertThat(result.gameState()).isEqualTo(GameState.CREATED);
    assertThat(result.roundsUnanswered()).isEqualTo(2);
    assertThat(result.score()).isZero();
  }

  @Test
  void scoreIsTheSumOfRoundPoints() {
    GameEntity game =
        game(
            hintedRound(3, GameType.TRIPOINT, AnswerState.GAVE_UP, 2),
            hintedRound(1, GameType.RIVERS_BY_COUNTRIES, AnswerState.CORRECT, 1),
            hintedRound(0, GameType.TRIPOINT, AnswerState.CORRECT, 0),
            hintedRound(2, GameType.TRIPOINT, AnswerState.CORRECT, 1),
            hintedRound(4, GameType.RIVERS_BY_SHAPE, AnswerState.UNANSWERED, 0));

    GameResult result = GameService.calculateGameResult(game);

    // perfect 100 + river with 1 of 2 hints 75 + tripoint with 1 of 3 hints 83 + revealed 0
    assertThat(result.roundPoints()).containsExactly(100, 75, 83, 0, 0);
    assertThat(result.roundHintsUsed()).containsExactly(0, 1, 1, 2, 0);
    assertThat(result.score()).isEqualTo(258);
    assertThat(result.maxScore()).isEqualTo(500);
    assertThat(result.roundsCorrect()).isEqualTo(3);
    assertThat(result.perfectRounds()).isEqualTo(1);
  }

  @Test
  void revealedRoundsScoreNothing() {
    GameEntity game = game(round(0, AnswerState.GAVE_UP), round(1, AnswerState.GAVE_UP));

    GameResult result = GameService.calculateGameResult(game);

    assertThat(result.score()).isZero();
    assertThat(result.maxScore()).isEqualTo(200);
  }

  @Test
  void revealedRoundScoresZeroWhateverTheHints() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse result =
        service.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 3, null).orElseThrow();

    assertThat(result.hintsUsed()).isEqualTo(3);
    assertThat(result.points()).isZero();
    assertThat(result.gameResult().score()).isZero();
  }

  @Test
  void outOfGuessesIsStoredScoresNothingAndIsNotCorrect() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse result =
        service.submitAnswer(GAME_ID, 0, AnswerState.OUT_OF_GUESSES, 1, null).orElseThrow();

    assertThat(game.getRounds().get(0).getAnswerState()).isEqualTo(AnswerState.OUT_OF_GUESSES);
    assertThat(result.answerState()).isEqualTo(AnswerState.OUT_OF_GUESSES);
    assertThat(result.points()).isZero();
    assertThat(result.gameResult().score()).isZero();
    assertThat(result.gameResult().roundsCorrect()).isZero();
    assertThat(result.gameResult().perfectRounds()).isZero();
    assertThat(result.gameResult().gameState()).isEqualTo(GameState.FINISHED);
  }

  @Test
  void submittingOutOfGuessesAgainChangesNothing() {
    GameEntity game = game(round(0, AnswerState.OUT_OF_GUESSES));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    RoundAnswerResponse again =
        service.submitAnswer(GAME_ID, 0, AnswerState.OUT_OF_GUESSES, 0, null).orElseThrow();

    assertThat(again.answerState()).isEqualTo(AnswerState.OUT_OF_GUESSES);
    verify(gameRepository, never()).save(any());
  }

  @Test
  void correctAfterOutOfGuessesIsRefused() {
    GameEntity game = game(round(0, AnswerState.OUT_OF_GUESSES));
    when(gameRepository.findByPublicId(GAME_ID)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null))
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
    assertThatThrownBy(() -> new GameEntity(null, "TRIPOINT"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ------------------------------------------------------------ recordGuess

  private GameRoundEntity guessRound(GameType type, AnswerState state) {
    GameRoundEntity entity = new GameRoundEntity(0, type, "{}");
    entity.setAnswerState(state);
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

  @ParameterizedTest
  @ValueSource(strings = {"Atlantis", "", "   "})
  void anUnknownOrBlankGuessIsRejected(String guess) {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);

    assertThatThrownBy(() -> service.recordGuess(GAME_ID, 0, guess, false))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(entity.getGuesses()).isEmpty();
  }

  @Test
  void aNullOrTooLongGuessIsRejected() {
    guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);

    assertThatThrownBy(() -> service.recordGuess(GAME_ID, 0, null, false))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.recordGuess(GAME_ID, 0, "a".repeat(256), false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void theLastAllowedWrongGuessIsAcceptedAndTheNextIsRejected() {
    guessRound(GameType.LAKES_BY_SHAPE, AnswerState.UNANSWERED);
    List<String> lakes = List.of("Garda", "Como", "Maggiore", "Lugano", "Iseo", "Orta");
    int limit = GameType.LAKES_BY_SHAPE.guessLimit();
    assertThat(lakes.size()).isGreaterThan(limit);

    for (int i = 0; i < limit; i++) {
      service.recordGuess(GAME_ID, 0, lakes.get(i), false);
    }

    assertThatThrownBy(() -> service.recordGuess(GAME_ID, 0, lakes.get(limit), false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("wrong");
  }

  @Test
  void moreCorrectGuessesThanAnswersIsRejected() {
    GameRoundEntity entity = guessRound(GameType.RIVERS_BY_SHAPE, AnswerState.UNANSWERED);
    entity.addGuess(new RoundGuess("Rhine", true));

    assertThatThrownBy(() -> service.recordGuess(GAME_ID, 0, "Rhein", true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("correct");
  }

  @Test
  void aGuessOnAnAnsweredRoundIsAConflict() {
    guessRound(GameType.TRIPOINT, AnswerState.CORRECT);

    assertThatThrownBy(() -> service.recordGuess(GAME_ID, 0, "France", false))
        .isInstanceOf(RoundAlreadyAnsweredForGuessException.class);
  }

  @Test
  void aRiverRoundAcceptsARiverAndRejectsACountry() {
    GameRoundEntity entity = guessRound(GameType.RIVERS_BY_SHAPE, AnswerState.UNANSWERED);

    service.recordGuess(GAME_ID, 0, "rhein", false);
    assertThatThrownBy(() -> service.recordGuess(GAME_ID, 0, "France", false))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(entity.getGuesses()).extracting(RoundGuess::getGuess).containsExactly("Rhein");
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
  void hintsAreRecordedInOrderWithTheGuessCountAtTheTime() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);
    service.recordGuess(GAME_ID, 0, "France", false);

    service.recordHint(GAME_ID, 0, 0, 0);
    RoundHintsResponse response = service.recordHint(GAME_ID, 0, 1, 1).orElseThrow();

    assertThat(response.hintsTakenAt()).containsExactly(0, 1);
    assertThat(entity.getHintsTakenAt()).containsExactly(0, 1);
  }

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
  void aSkippedHintAHintPastTheCountAndANegativeCountAreRejected() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);

    assertThatThrownBy(() -> service.recordHint(GAME_ID, 0, 1, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.recordHint(GAME_ID, 0, 0, -1))
        .isInstanceOf(IllegalArgumentException.class);
    for (int i = 0; i < GameType.TRIPOINT.hintCount(); i++) {
      service.recordHint(GAME_ID, 0, i, 0);
    }
    int past = GameType.TRIPOINT.hintCount();
    assertThatThrownBy(() -> service.recordHint(GAME_ID, 0, past, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("hints");
    assertThat(entity.getHintsTakenAt()).hasSize(past);
  }

  @Test
  void aGuessCountLargerThanTheStoredGuessesIsClamped() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);
    service.recordGuess(GAME_ID, 0, "France", false);

    service.recordHint(GAME_ID, 0, 0, 4);

    assertThat(entity.getHintsTakenAt()).containsExactly(1);
  }

  @Test
  void aHintOnAnAnsweredRoundIsAConflict() {
    guessRound(GameType.TRIPOINT, AnswerState.GAVE_UP);

    assertThatThrownBy(() -> service.recordHint(GAME_ID, 0, 0, 0))
        .isInstanceOf(RoundAlreadyAnsweredForGuessException.class);
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
        service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null).orElseThrow();

    assertThat(entity.getHintsUsed()).isEqualTo(2);
    assertThat(result.hintsUsed()).isEqualTo(2);
    assertThat(result.points())
        .isEqualTo(Scoring.roundPoints(AnswerState.CORRECT, 2, GameType.TRIPOINT.hintCount()));
    ArgumentCaptor<RoundAnswered> event = ArgumentCaptor.forClass(RoundAnswered.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().hintsUsed()).isEqualTo(2);
  }

  @Test
  void aRepeatOfThatAnswerIsANoOp() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);
    entity.addHint(0);
    service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null);

    RoundAnswerResponse again =
        service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null).orElseThrow();

    assertThat(again.hintsUsed()).isEqualTo(1);
    verify(events, times(1)).publishEvent(any(Object.class));
  }

  @Test
  void anAnswerReportingMoreHintsThanRecordedIsAccepted() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);
    entity.addHint(0);

    service.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 2, null);

    assertThat(entity.getHintsUsed()).isEqualTo(2);
  }
}
