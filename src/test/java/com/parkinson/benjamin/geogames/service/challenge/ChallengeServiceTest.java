package com.parkinson.benjamin.geogames.service.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.api.BadRequestException;
import com.parkinson.benjamin.geogames.api.ChallengeEntryView;
import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.ChallengeLink;
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
          metrics,
          new Initials(new String[] {"BAD"}));

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
    when(entries.top(5, 5)).thenReturn(List.of(entry(3, 150, "ABC")));
    when(entries.count(5)).thenReturn(1);

    var response = service.find(id);

    assertThat(response.entries()).containsExactly(new ChallengeEntryView(1, 150, "ABC", null));
    assertThat(response.entryCount()).isEqualTo(1);

    assertThat(response.gameMode()).isEqualTo("RANDOM");
    assertThat(response.difficulty()).isEqualTo("EASY");
    assertThat(response.roundCount()).isEqualTo(2);
    assertThat(response.score()).isEqualTo(150);
    assertThat(response.maxScore()).isEqualTo(200);
    verify(store, never()).touch(anyLong(), any());
    verify(metrics).challengeOpened(true);
  }

  private UUID linkedGame(Long challengeId, Long entryId, boolean creator) {
    UUID gameId = UUID.randomUUID();
    when(games.challengeLink(gameId))
        .thenReturn(Optional.of(new ChallengeLink(challengeId, entryId, creator)));
    return gameId;
  }

  private static ChallengeEntryStore.Entry entry(long id, int score, String initials) {
    return new ChallengeEntryStore.Entry(id, score, initials);
  }

  /** Five of six entries, the second and third tied. */
  private void tableOfSix(UUID challengeId) {
    when(store.findById(5)).thenReturn(Optional.of(stored(challengeId, Difficulty.HARD)));
    when(entries.top(5, 5))
        .thenReturn(
            List.of(
                entry(3, 190, "ABC"),
                entry(1, 150, null),
                entry(4, 150, null),
                entry(2, 100, null),
                entry(5, 90, null)));
    when(entries.count(5)).thenReturn(6);
  }

  @Test
  void tableRanksTheEntriesInOrderAndMarksTheGamesOwn() {
    UUID challengeId = UUID.randomUUID();
    tableOfSix(challengeId);

    var table = service.table(linkedGame(5L, 4L, false));

    assertThat(table.challengeId()).isEqualTo(challengeId);
    assertThat(table.maxScore()).isEqualTo(200);
    assertThat(table.entryCount()).isEqualTo(6);
    assertThat(table.creator()).isFalse();
    assertThat(table.entries())
        .extracting(ChallengeEntryView::rank, ChallengeEntryView::score, ChallengeEntryView::you)
        .containsExactly(
            tuple(1, 190, false),
            tuple(2, 150, false),
            tuple(3, 150, true),
            tuple(4, 100, false),
            tuple(5, 90, false));
    assertThat(table.yours()).isNull();
    assertThat(table.canEnterInitials()).isTrue();
  }

  @Test
  void tableAddsTheGamesRowWhenItIsBelowTheTopFive() {
    tableOfSix(UUID.randomUUID());
    when(entries.find(6)).thenReturn(Optional.of(entry(6, 10, null)));
    when(entries.rank(6)).thenReturn(6);

    var table = service.table(linkedGame(5L, 6L, true));

    assertThat(table.creator()).isTrue();
    assertThat(table.yours()).isEqualTo(new ChallengeEntryView(6, 10, null, true));
    assertThat(table.canEnterInitials()).isFalse();
  }

  @Test
  void tableOfAnUnfinishedGameHasNoRowOfItsOwn() {
    tableOfSix(UUID.randomUUID());

    var table = service.table(linkedGame(5L, null, false));

    assertThat(table.entries()).extracting(ChallengeEntryView::you).containsOnly(false);
    assertThat(table.yours()).isNull();
    assertThat(table.canEnterInitials()).isFalse();
  }

  @Test
  void aGameWithoutAChallengeHasNoTable() {
    UUID unknown = UUID.randomUUID();
    when(games.challengeLink(unknown)).thenReturn(Optional.empty());
    UUID never = linkedGame(null, null, true);
    UUID expired = linkedGame(4L, null, false);
    when(store.findById(4)).thenReturn(Optional.empty());

    for (UUID gameId : List.of(unknown, never, expired)) {
      assertThatThrownBy(() -> service.table(gameId))
          .isInstanceOf(ChallengeNotFoundException.class);
    }
  }

  @Test
  void initialsBlockedSinceTheyWereSetAreNotShown() {
    UUID id = UUID.randomUUID();
    when(store.find(id)).thenReturn(Optional.of(stored(id, Difficulty.HARD)));
    when(store.findById(5)).thenReturn(Optional.of(stored(id, Difficulty.HARD)));
    when(entries.top(5, 5)).thenReturn(List.of(entry(3, 190, "BAD")));
    when(entries.find(6)).thenReturn(Optional.of(entry(6, 10, "BAD")));
    when(entries.rank(6)).thenReturn(6);

    var table = service.table(linkedGame(5L, 6L, false));

    assertThat(table.entries()).extracting(ChallengeEntryView::initials).containsOnlyNulls();
    assertThat(table.yours().initials()).isNull();
    assertThat(service.find(id).entries())
        .extracting(ChallengeEntryView::initials)
        .containsOnlyNulls();
  }

  @Test
  void initialsAreStoredUpperCasedForATopFiveEntry() {
    when(entries.rank(6)).thenReturn(5);

    service.setInitials(linkedGame(5L, 6L, false), " ab ");

    verify(entries).setInitials(6, "AB");
  }

  @Test
  void initialsAreRefusedBelowTheTopFiveWithoutAnEntryOrWhenInvalid() {
    when(entries.rank(6)).thenReturn(6);
    UUID low = linkedGame(5L, 6L, false);
    UUID noEntry = linkedGame(5L, null, false);

    assertThatThrownBy(() -> service.setInitials(low, "AB"))
        .isInstanceOf(ChallengeConflictException.class);
    assertThatThrownBy(() -> service.setInitials(noEntry, "AB"))
        .isInstanceOf(ChallengeNotFoundException.class);
    assertThatThrownBy(() -> service.setInitials(low, "A1"))
        .isInstanceOf(BadRequestException.class);
    verify(entries, never()).setInitials(anyLong(), any());
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
