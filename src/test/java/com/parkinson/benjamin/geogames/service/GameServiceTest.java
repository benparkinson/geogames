package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.dao.GameData;
import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.dao.GameType;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.GameCreationResponse;
import com.parkinson.benjamin.geogames.model.GameResult;
import com.parkinson.benjamin.geogames.model.GameRound;
import com.parkinson.benjamin.geogames.model.GameState;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
  private TripointFinderService tripointFinderService;
  private RiverLoaderService riverLoaderService;
  private RiverFinderService riverFinderService;
  private GameService service;

  @BeforeEach
  void setUp() {
    gameRepository = mock(GameRepository.class);
    tripointFinderService = mock(TripointFinderService.class);
    riverLoaderService = mock(RiverLoaderService.class);
    riverFinderService = mock(RiverFinderService.class);
    service =
        new GameService(
            gameRepository,
            tripointFinderService,
            riverLoaderService,
            riverFinderService,
            objectMapper);

    when(gameRepository.save(any(GameEntity.class)))
        .thenAnswer(
            invocation -> {
              GameEntity game = invocation.getArgument(0);
              game.setId(SAVED_ID);
              return game;
            });
  }

  // ---------------------------------------------------------------- fixtures

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

  private static River river(int n) {
    List<List<List<Double>>> lines = List.of(List.of(List.of(0.0, 0.0), List.of(1.0, 1.0)));
    GeoData geoData =
        new GeoData(
            "Feature", new GeoProperties("River" + n), new GeoGeometry("MultiLineString", lines));
    return new River("River" + n, List.of("Alias" + n), "Africa", geoData);
  }

  private List<GameData> tripoints(int count) {
    return IntStream.range(0, count).<GameData>mapToObj(GameServiceTest::tripoint).toList();
  }

  private List<GameData> rivers(int count) {
    return IntStream.range(0, count).<GameData>mapToObj(GameServiceTest::river).toList();
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
  void acceptsBoundaryRoundCounts(int rounds) throws IOException {
    when(tripointFinderService.findRandomTripoints(rounds)).thenReturn(tripoints(rounds));

    assertThat(service.createGame("TRIPOINT", rounds)).isPresent();
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
  void createsTripointRounds() throws IOException {
    when(tripointFinderService.findRandomTripoints(3)).thenReturn(tripoints(3));

    Optional<GameCreationResponse> response = service.createGame("TRIPOINT", 3);

    assertThat(response).contains(new GameCreationResponse(SAVED_ID));
    verify(tripointFinderService, times(1)).findRandomTripoints(3);
    verifyNoInteractions(riverLoaderService, riverFinderService);

    List<GameRoundEntity> rounds = savedGame().getRounds();
    assertThat(rounds).hasSize(3);
    for (int i = 0; i < 3; i++) {
      GameRoundEntity round = rounds.get(i);
      assertThat(round.getIndex()).isEqualTo(i);
      assertThat(round.getGameType()).isSameAs(GameType.TRIPOINT);
      assertThat(round.getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
      JsonNode blob = parse(round.getJsonBlob());
      assertThat(blob.get("countries").size()).isEqualTo(3);
      assertThat(blob.has("coordinate")).isTrue();
    }
  }

  @Test
  void createsRiverRounds() throws IOException {
    List<River> loaded = List.of(river(0), river(1), river(2));
    when(riverLoaderService.loadRivers()).thenReturn(loaded);
    when(riverFinderService.findRandomRivers(loaded, 2)).thenReturn(rivers(2));

    service.createGame("RIVERS_BY_SHAPE", 2);

    verify(riverFinderService).findRandomRivers(loaded, 2);
    verifyNoInteractions(tripointFinderService);

    List<GameRoundEntity> rounds = savedGame().getRounds();
    assertThat(rounds).hasSize(2);
    for (int i = 0; i < 2; i++) {
      GameRoundEntity round = rounds.get(i);
      assertThat(round.getIndex()).isEqualTo(i);
      assertThat(round.getGameType()).isSameAs(GameType.RIVERS_BY_SHAPE);
      assertThat(round.getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
      JsonNode blob = parse(round.getJsonBlob());
      assertThat(blob.has("name")).isTrue();
      assertThat(blob.has("additionalNames")).isTrue();
      assertThat(blob.has("continent")).isTrue();
      assertThat(blob.has("geoData")).isTrue();
    }
  }

  @RepeatedTest(20)
  void createsRandomRoundsSplitAcrossBothTypes() throws IOException {
    int total = 6;
    when(tripointFinderService.findRandomTripoints(anyInt()))
        .thenAnswer(invocation -> tripoints(invocation.getArgument(0)));
    when(riverLoaderService.loadRivers()).thenReturn(List.of(river(0)));
    when(riverFinderService.findRandomRivers(any(), anyInt()))
        .thenAnswer(invocation -> rivers(invocation.getArgument(1)));

    service.createGame("RANDOM", total);

    ArgumentCaptor<Integer> tripointCount = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> riverCount = ArgumentCaptor.forClass(Integer.class);
    verify(tripointFinderService).findRandomTripoints(tripointCount.capture());
    verify(riverFinderService).findRandomRivers(any(), riverCount.capture());
    assertThat(tripointCount.getValue() + riverCount.getValue()).isEqualTo(total);

    List<GameRoundEntity> rounds = savedGame().getRounds();
    assertThat(rounds).hasSize(total);
    assertThat(rounds.stream().map(GameRoundEntity::getIndex).toList())
        .containsExactly(0, 1, 2, 3, 4, 5);
    assertThat(rounds.stream().filter(r -> r.getGameType() == GameType.TRIPOINT).count())
        .isEqualTo((long) tripointCount.getValue());
    assertThat(rounds.stream().filter(r -> r.getGameType() == GameType.RIVERS_BY_SHAPE).count())
        .isEqualTo((long) riverCount.getValue());
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

    assertThat(result.gameType()).isSameAs(GameType.RIVERS_BY_SHAPE);
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

    // Current behaviour: a round that was answered CORRECT can later be changed to GAVE_UP, and
    // even reset to UNANSWERED, because the service never checks the existing state.
    GameRound changed = service.submitAnswer(1L, 0, AnswerState.GAVE_UP).orElseThrow();
    assertThat(changed.answerState()).isEqualTo(AnswerState.GAVE_UP);
    assertThat(changed.gameResult().score()).isZero();

    GameRound reset = service.submitAnswer(1L, 0, AnswerState.UNANSWERED).orElseThrow();
    assertThat(reset.answerState()).isEqualTo(AnswerState.UNANSWERED);
    assertThat(reset.gameResult().gameState()).isEqualTo(GameState.CREATED);
    verify(gameRepository, times(2)).save(eq(game));
  }

  // ------------------------------------------------------ calculateGameResult

  @Test
  void roundAnswerStatesAreOrderedByIndexAndScored() {
    GameEntity game =
        game(
            round(2, AnswerState.UNANSWERED),
            round(0, AnswerState.CORRECT),
            round(1, AnswerState.GAVE_UP));

    GameResult result = GameService.calculateGameResult(Optional.of(game));

    assertThat(result.roundAnswerStates())
        .containsExactly(AnswerState.CORRECT, AnswerState.GAVE_UP, AnswerState.UNANSWERED);
    assertThat(result.score()).isEqualTo(1);
    assertThat(result.roundsUnanswered()).isEqualTo(1);
    assertThat(result.gameState()).isEqualTo(GameState.IN_PROGRESS);
  }

  @Test
  void gameIsFinishedWhenEveryRoundIsAnswered() {
    GameEntity game = game(round(1, AnswerState.GAVE_UP), round(0, AnswerState.CORRECT));

    GameResult result = GameService.calculateGameResult(Optional.of(game));

    assertThat(result.gameState()).isEqualTo(GameState.FINISHED);
    assertThat(result.roundsUnanswered()).isZero();
    assertThat(result.roundAnswerStates())
        .containsExactly(AnswerState.CORRECT, AnswerState.GAVE_UP);
  }

  @Test
  void gameIsCreatedWhenNothingIsAnswered() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED), round(1, AnswerState.UNANSWERED));

    GameResult result = GameService.calculateGameResult(Optional.of(game));

    assertThat(result.gameState()).isEqualTo(GameState.CREATED);
    assertThat(result.roundsUnanswered()).isEqualTo(2);
    assertThat(result.score()).isZero();
  }

  @Test
  void scoreCountsOnlyCorrectAnswers() {
    GameEntity game = game(round(0, AnswerState.GAVE_UP), round(1, AnswerState.GAVE_UP));

    assertThat(GameService.calculateGameResult(Optional.of(game)).score()).isZero();
  }

  @Test
  void missingGameCountsAsFinishedWithNothing() {
    // Current behaviour: no game at all is reported as FINISHED (zero rounds, none unanswered)
    // rather than as an error or an empty result.
    GameResult result = GameService.calculateGameResult(Optional.empty());

    assertThat(result.gameState()).isEqualTo(GameState.FINISHED);
    assertThat(result.roundsUnanswered()).isZero();
    assertThat(result.score()).isZero();
    assertThat(result.roundAnswerStates()).isEmpty();
  }
}
