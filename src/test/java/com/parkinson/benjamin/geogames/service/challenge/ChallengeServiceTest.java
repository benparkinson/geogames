package com.parkinson.benjamin.geogames.service.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.api.BadRequestException;
import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.ChallengeSource;
import com.parkinson.benjamin.geogames.service.GameService;
import com.parkinson.benjamin.geogames.service.daily.PinnedRound;
import com.parkinson.benjamin.geogames.service.metrics.GameMetrics;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChallengeServiceTest {

  private static final List<PinnedRound> ROUNDS =
      List.of(
          new PinnedRound(GameType.TRIPOINT, "tripoint:a"),
          new PinnedRound(GameType.LAKES_BY_SHAPE, "lake:Q1"));

  private final ChallengeStore store = mock(ChallengeStore.class);
  private final ChallengeEntryStore entries = mock(ChallengeEntryStore.class);
  private final GameService games = mock(GameService.class);
  private final GameMetrics metrics = mock(GameMetrics.class);
  private final ChallengeService service =
      new ChallengeService(
          Clock.fixed(Instant.parse("2026-11-25T10:15:30Z"), ZoneOffset.UTC),
          store,
          entries,
          games,
          metrics);

  private static final LocalDate TODAY = LocalDate.parse("2026-11-25");

  private static StoredChallenge stored(UUID id, Difficulty difficulty) {
    return new StoredChallenge(
        5, new Challenge(id, "RANDOM", difficulty, ROUNDS, 200, TODAY, TODAY));
  }

  private UUID finishedGame(Long challengeId) {
    UUID gameId = UUID.randomUUID();
    when(games.challengeSource(gameId))
        .thenReturn(
            Optional.of(
                new ChallengeSource(ROUNDS, "RANDOM", Difficulty.HARD, 150, 200, challengeId)));
    return gameId;
  }

  @Test
  void createCopiesTheGameAndEntersItsScore() {
    UUID gameId = finishedGame(null);
    when(store.insert(any())).thenReturn(5L);
    when(entries.insert(5, 150)).thenReturn(6L);

    UUID id = service.create(gameId).id();

    ArgumentCaptor<Challenge> inserted = ArgumentCaptor.forClass(Challenge.class);
    verify(store).insert(inserted.capture());
    assertThat(inserted.getValue())
        .isEqualTo(new Challenge(id, "RANDOM", Difficulty.HARD, ROUNDS, 200, TODAY, TODAY));
    verify(games).linkChallenge(gameId, 5, 6);
    verify(metrics).challengeCreated("RANDOM", Difficulty.HARD);
  }

  @Test
  void createReusesTheGamesChallengeAndTouchesIt() {
    UUID gameId = finishedGame(5L);
    UUID existing = UUID.randomUUID();
    when(store.findById(5)).thenReturn(Optional.of(stored(existing, Difficulty.HARD)));

    assertThat(service.create(gameId).id()).isEqualTo(existing);

    verify(store).touch(5, TODAY);
    verify(store, never()).insert(any());
    verifyNoInteractions(entries, metrics);
  }

  @Test
  void createMakesANewChallengeWhenTheGamesOwnHasGone() {
    UUID gameId = finishedGame(4L);
    when(store.findById(4)).thenReturn(Optional.empty());
    when(store.insert(any())).thenReturn(5L);
    when(entries.insert(5, 150)).thenReturn(6L);

    service.create(gameId);

    verify(games).linkChallenge(gameId, 5, 6);
    verify(metrics).challengeCreated("RANDOM", Difficulty.HARD);
  }

  @Test
  void createRejectsWhatTheGameServiceRejects() {
    UUID unknown = UUID.randomUUID();
    when(games.challengeSource(unknown)).thenReturn(Optional.empty());
    UUID daily = UUID.randomUUID();
    when(games.challengeSource(daily)).thenThrow(new BadRequestException("daily"));
    UUID unfinished = UUID.randomUUID();
    when(games.challengeSource(unfinished)).thenThrow(new ChallengeConflictException("open"));

    assertThatThrownBy(() -> service.create(unknown))
        .isInstanceOf(ChallengeNotFoundException.class);
    assertThatThrownBy(() -> service.create(daily)).isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> service.create(unfinished))
        .isInstanceOf(ChallengeConflictException.class);
  }

  @Test
  void findDescribesTheChallengeWithTheTopScoreAndWithoutTheRounds() {
    UUID id = UUID.randomUUID();
    when(store.find(id)).thenReturn(Optional.of(stored(id, Difficulty.EASY)));
    when(entries.topScore(5)).thenReturn(150);

    var response = service.find(id);

    assertThat(response.gameMode()).isEqualTo("RANDOM");
    assertThat(response.difficulty()).isEqualTo("EASY");
    assertThat(response.roundCount()).isEqualTo(2);
    assertThat(response.score()).isEqualTo(150);
    assertThat(response.maxScore()).isEqualTo(200);
    verify(store, never()).touch(anyLong(), any());
    verify(metrics).challengeOpened(true);
  }

  @Test
  void anUnknownOrExpiredChallengeIsNotFound() {
    UUID id = UUID.randomUUID();
    when(store.find(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.find(id)).isInstanceOf(ChallengeNotFoundException.class);
    assertThatThrownBy(() -> service.createGame(id)).isInstanceOf(ChallengeNotFoundException.class);
    // Only opening the link is counted, not the attempt to start it.
    verify(metrics).challengeOpened(false);
    verifyNoMoreInteractions(metrics);
  }

  @Test
  void createGameTouchesTheChallengeAndSetsTheTopScoreToBeat() {
    UUID id = UUID.randomUUID();
    when(store.find(id)).thenReturn(Optional.of(stored(id, Difficulty.HARD)));
    when(entries.topScore(5)).thenReturn(180);
    GameCreationResponse created = new GameCreationResponse(UUID.randomUUID());
    when(games.createGame(any(), any(), any(), any(), any(), any())).thenReturn(created);

    assertThat(service.createGame(id)).isSameAs(created);

    verify(store).touch(5, TODAY);
    verify(games).createGame(ROUNDS, "RANDOM", Difficulty.HARD, null, 180, 5L);
    verify(metrics).challengeStarted("RANDOM", Difficulty.HARD);
  }
}
