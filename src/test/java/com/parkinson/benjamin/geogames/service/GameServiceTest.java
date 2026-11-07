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
import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameState;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.RiverRound;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class GameServiceTest {

  private static final long SAVED_ID = 42L;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  private GameRepository gameRepository;
  private RoundGenerator tripointGenerator;
  private RoundGenerator riverGenerator;
  private final Map<GameType, RoundGenerator> generators = new EnumMap<>(GameType.class);
  private GameService service;

  @BeforeEach
  void setUp() {
    gameRepository = mock(GameRepository.class);
    for (GameType type : GameType.values()) {
      RoundGenerator generator = generatorFor(type);
      when(generator.generate(anyInt(), any()))
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
              game.setId(SAVED_ID);
              return game;
            });
  }

  // ---------------------------------------------------------------- fixtures

  private static RoundGenerator generatorFor(GameType type) {
    RoundGenerator generator = mock(RoundGenerator.class);
    when(generator.gameType()).thenReturn(type);
    return generator;
  }

  private GameService allGeneratorsService(Random random) {
    return new GameService(gameRepository, List.copyOf(generators.values()), objectMapper, random);
  }

  private List<GameData> roundsOf(GameType type, int count) {
    return type == GameType.TRIPOINT ? tripoints(count) : rivers(count, type);
  }

  private GameService serviceWith(Random random, RoundGenerator... generators) {
    return new GameService(gameRepository, List.of(generators), objectMapper, random);
  }

  private static GameRoundEntity round(int index, AnswerState state) {
    GameRoundEntity entity = new GameRoundEntity(index, GameType.TRIPOINT, "{}");
    entity.setAnswerState(state);
    return entity;
  }

  private static GameEntity game(GameRoundEntity... rounds) {
    return new GameEntity(new ArrayList<>(List.of(rounds)));
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
    when(tripointGenerator.generate(rounds, Set.of())).thenReturn(tripoints(rounds));

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
    when(tripointGenerator.generate(3, Set.of())).thenReturn(tripoints(3));

    GameCreationResponse response = service.createGame("TRIPOINT", 3);

    assertThat(response).isEqualTo(new GameCreationResponse(SAVED_ID));
    verify(tripointGenerator, times(1)).generate(3, Set.of());
    verify(riverGenerator, never()).generate(anyInt(), any());

    List<GameRoundEntity> rounds = savedGame().getRounds();
    assertThat(rounds).hasSize(3);
    for (int i = 0; i < 3; i++) {
      GameRoundEntity round = rounds.get(i);
      assertThat(round.getRoundNumber()).isEqualTo(i);
      assertThat(round.getGameType()).isSameAs(GameType.TRIPOINT);
      assertThat(round.getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
      JsonNode blob = parse(round.getJsonBlob());
      assertThat(blob.get("countries").size()).isEqualTo(3);
      assertThat(blob.has("coordinate")).isTrue();
    }
  }

  @Test
  void createsRiverRounds() {
    when(riverGenerator.generate(2, Set.of())).thenReturn(rivers(2));

    service.createGame("RIVERS_BY_SHAPE", 2);

    verify(riverGenerator).generate(2, Set.of());
    verify(tripointGenerator, never()).generate(anyInt(), any());

    List<GameRoundEntity> rounds = savedGame().getRounds();
    assertThat(rounds).hasSize(2);
    for (int i = 0; i < 2; i++) {
      GameRoundEntity round = rounds.get(i);
      assertThat(round.getRoundNumber()).isEqualTo(i);
      assertThat(round.getGameType()).isSameAs(GameType.RIVERS_BY_SHAPE);
      assertThat(round.getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
      JsonNode blob = parse(round.getJsonBlob());
      assertThat(blob.get("river").has("name")).isTrue();
      assertThat(blob.get("gameType").asString()).isEqualTo("RIVERS_BY_SHAPE");
      assertThat(blob.get("river").has("additionalNames")).isTrue();
      assertThat(blob.get("river").has("continent")).isTrue();
      assertThat(blob.get("river").has("geoData")).isTrue();
      assertThat(blob.get("countries").size()).isZero();
    }
  }

  @RepeatedTest(20)
  void createsRandomRoundsSplitAcrossEveryType() {
    int total = 6;
    service = allGeneratorsService(new Random());

    service.createGame("RANDOM", total);

    Map<GameType, Integer> requested = new EnumMap<>(GameType.class);
    for (GameType type : GameType.values()) {
      ArgumentCaptor<Integer> count = ArgumentCaptor.forClass(Integer.class);
      verify(generators.get(type), atMost(1)).generate(count.capture(), any());
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
    when(tripointGenerator.generate(3, Set.of()))
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
      new GameService(repository, List.copyOf(generators.values()), objectMapper, new Random(7))
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
      when(generators.get(type).generate(anyInt(), any()))
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
  void getGameRoundIsEmptyForUnknownGame() {
    when(gameRepository.findById(1L)).thenReturn(Optional.empty());

    assertThat(service.getGameRound(1L, 0)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 2, 99})
  void getGameRoundIsEmptyForUnknownRound(int index) {
    when(gameRepository.findById(1L))
        .thenReturn(
            Optional.of(game(round(0, AnswerState.UNANSWERED), round(1, AnswerState.CORRECT))));

    assertThat(service.getGameRound(1L, index)).isEmpty();
  }

  @Test
  void getGameRoundReturnsTheRequestedRound() {
    GameRoundEntity first = new GameRoundEntity(0, GameType.TRIPOINT, "{\"first\":true}");
    GameRoundEntity second = new GameRoundEntity(1, GameType.RIVERS_BY_SHAPE, "{\"second\":true}");
    second.setAnswerState(AnswerState.GAVE_UP);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game(first, second)));

    GameRound result = service.getGameRound(1L, 1).orElseThrow();

    assertThat(result.gameType()).isEqualTo(GameTypeResponse.from(GameType.RIVERS_BY_SHAPE));
    assertThat(result.jsonBlob()).isEqualTo("{\"second\":true}");
    assertThat(result.answerState()).isEqualTo(AnswerState.GAVE_UP);
    assertThat(result.totalRoundCount()).isEqualTo(2);
    assertThat(result.gameResult())
        .isEqualTo(
            new GameResult(
                GameState.IN_PROGRESS, 1, 0, List.of(AnswerState.UNANSWERED, AnswerState.GAVE_UP)));
  }

  // ------------------------------------------------------------ submitAnswer

  @Test
  void submitAnswerIsEmptyForUnknownGameOrRound() {
    when(gameRepository.findById(1L)).thenReturn(Optional.empty());
    when(gameRepository.findById(2L))
        .thenReturn(Optional.of(game(round(0, AnswerState.UNANSWERED))));

    assertThat(service.submitAnswer(1L, 0, AnswerState.CORRECT)).isEmpty();
    assertThat(service.submitAnswer(2L, 5, AnswerState.CORRECT)).isEmpty();
    verify(gameRepository, never()).save(any());
  }

  @Test
  void submitAnswerUpdatesOnlyThatRoundAndAdvancesTheGame() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED), round(1, AnswerState.UNANSWERED));
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

    GameRound afterFirst = service.submitAnswer(1L, 0, AnswerState.CORRECT).orElseThrow();

    assertThat(afterFirst.answerState()).isEqualTo(AnswerState.CORRECT);
    assertThat(afterFirst.gameResult().gameState()).isEqualTo(GameState.IN_PROGRESS);
    assertThat(afterFirst.gameResult().score()).isEqualTo(1);
    assertThat(game.getRounds().get(1).getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
    verify(gameRepository, times(1)).save(game);

    GameRound afterSecond = service.submitAnswer(1L, 1, AnswerState.GAVE_UP).orElseThrow();

    assertThat(afterSecond.gameResult().gameState()).isEqualTo(GameState.FINISHED);
    assertThat(afterSecond.gameResult().score()).isEqualTo(1);
    assertThat(afterSecond.gameResult().roundAnswerStates())
        .containsExactly(AnswerState.CORRECT, AnswerState.GAVE_UP);
    verify(gameRepository, times(2)).save(game);
  }

  @Test
  void submitAnswerOverwritesAnAlreadyAnsweredRound() {
    GameEntity game = game(round(0, AnswerState.CORRECT));
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

    // TODO: a round that was answered CORRECT can later be changed to GAVE_UP, because the
    // service never checks the existing state. Making answers final needs a frontend change too.
    GameRound changed = service.submitAnswer(1L, 0, AnswerState.GAVE_UP).orElseThrow();
    assertThat(changed.answerState()).isEqualTo(AnswerState.GAVE_UP);
    assertThat(changed.gameResult().score()).isZero();
    verify(gameRepository, times(1)).save(eq(game));
  }

  @Test
  void submitAnswerRejectsUnansweredAndLeavesTheRoundAlone() {
    GameEntity game = game(round(0, AnswerState.CORRECT));
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> service.submitAnswer(1L, 0, AnswerState.UNANSWERED))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("UNANSWERED");

    assertThat(game.getRounds().get(0).getAnswerState()).isEqualTo(AnswerState.CORRECT);
    verify(gameRepository, never()).save(any());
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
    assertThat(result.score()).isEqualTo(1);
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
  void scoreCountsOnlyCorrectAnswers() {
    GameEntity game = game(round(0, AnswerState.GAVE_UP), round(1, AnswerState.GAVE_UP));

    assertThat(GameService.calculateGameResult(game).score()).isZero();
  }

  @Test
  void aGameWithNoRoundsCountsAsFinishedWithNothing() {
    // TODO: a game with no rounds is reported as FINISHED (none unanswered) rather than as an
    // error or an empty result. createGame never makes one, since it needs at least one round.
    GameResult result = GameService.calculateGameResult(game());

    assertThat(result.gameState()).isEqualTo(GameState.FINISHED);
    assertThat(result.roundsUnanswered()).isZero();
    assertThat(result.score()).isZero();
    assertThat(result.roundAnswerStates()).isEmpty();
  }
}
