package com.parkinson.benjamin.geogames.service.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.api.BadRequestException;
import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.ChallengeSource;
import com.parkinson.benjamin.geogames.service.GameService;
import com.parkinson.benjamin.geogames.service.daily.PinnedRound;
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
  private final GameService games = mock(GameService.class);
  private final ChallengeService service =
      new ChallengeService(
          Clock.fixed(Instant.parse("2026-11-25T10:15:30Z"), ZoneOffset.UTC), store, games);

  @Test
  void createCopiesTheRoundKeysModeDifficultyAndScore() {
    UUID gameId = UUID.randomUUID();
    when(games.challengeSource(gameId))
        .thenReturn(Optional.of(new ChallengeSource(ROUNDS, "RANDOM", Difficulty.HARD, 150, 200)));

    UUID id = service.create(gameId).id();

    ArgumentCaptor<Challenge> stored = ArgumentCaptor.forClass(Challenge.class);
    verify(store).insert(stored.capture());
    assertThat(stored.getValue())
        .isEqualTo(
            new Challenge(
                id, "RANDOM", Difficulty.HARD, ROUNDS, 150, 200, LocalDate.parse("2026-11-25")));
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
  void findDescribesTheChallengeWithoutTheRounds() {
    UUID id = UUID.randomUUID();
    when(store.find(id))
        .thenReturn(
            Optional.of(
                new Challenge(
                    id,
                    "RANDOM",
                    Difficulty.EASY,
                    ROUNDS,
                    150,
                    200,
                    LocalDate.parse("2026-11-25"))));

    var response = service.find(id);

    assertThat(response.gameMode()).isEqualTo("RANDOM");
    assertThat(response.difficulty()).isEqualTo("EASY");
    assertThat(response.roundCount()).isEqualTo(2);
    assertThat(response.score()).isEqualTo(150);
    assertThat(response.maxScore()).isEqualTo(200);
  }

  @Test
  void anUnknownOrExpiredChallengeIsNotFound() {
    UUID id = UUID.randomUUID();
    when(store.find(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.find(id)).isInstanceOf(ChallengeNotFoundException.class);
    assertThatThrownBy(() -> service.createGame(id)).isInstanceOf(ChallengeNotFoundException.class);
  }

  @Test
  void createGameBuildsANewGameFromTheStoredRounds() {
    UUID id = UUID.randomUUID();
    when(store.find(id))
        .thenReturn(
            Optional.of(
                new Challenge(
                    id, "RANDOM", Difficulty.HARD, ROUNDS, 1, 2, LocalDate.parse("2026-11-25"))));
    GameCreationResponse created = new GameCreationResponse(UUID.randomUUID());
    when(games.createGame(any(), any(), any(), any())).thenReturn(created);

    assertThat(service.createGame(id)).isSameAs(created);

    verify(games).createGame(ROUNDS, "RANDOM", Difficulty.HARD, null);
  }
}
