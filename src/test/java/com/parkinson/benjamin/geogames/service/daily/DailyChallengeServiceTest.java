package com.parkinson.benjamin.geogames.service.daily;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.api.DailyGameResponse;
import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.GameService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class DailyChallengeServiceTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);
  private static final List<PinnedRound> ROUNDS =
      List.of(new PinnedRound(GameType.TRIPOINT, "tripoint:x"));

  private final DailyChallengeStore store = mock(DailyChallengeStore.class);
  private final GameService games = mock(GameService.class);
  private final DailyScoreStore scores = mock(DailyScoreStore.class);
  private final UUID gameId = UUID.randomUUID();

  private DailyChallengeService service(LocalDate utcToday) {
    Clock clock = Clock.fixed(utcToday.atTime(23, 59).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    when(store.findOrCreate(any(), any())).thenReturn(ROUNDS);
    when(games.createGame(eq(ROUNDS), eq("DAILY"), any(), any()))
        .thenReturn(new GameCreationResponse(gameId));
    return new DailyChallengeService(
        clock, mock(DailyRoundPicker.class), store, mock(DailyHistoryLoader.class), games, scores);
  }

  private static DailyGameResponse createDailyGame(
      DailyChallengeService service, Optional<LocalDate> date) {
    return service.createDailyGame(date, null);
  }

  @Test
  void yesterdayTodayAndTomorrowAreAccepted() {
    DailyChallengeService service = service(TODAY);

    for (int offset = -1; offset <= 1; offset++) {
      LocalDate date = TODAY.plusDays(offset);
      DailyGameResponse response = createDailyGame(service, Optional.of(date));

      assertThat(response.date()).isEqualTo(date);
      assertThat(response.id()).isEqualTo(gameId);
    }
  }

  @Test
  void twoDaysAwayIsRejected() {
    DailyChallengeService service = service(TODAY);

    assertThatThrownBy(() -> createDailyGame(service, Optional.of(TODAY.plusDays(2))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> createDailyGame(service, Optional.of(TODAY.minusDays(2))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aDateBeforeTheEpochIsRejected() {
    DailyChallengeService service = service(DailyChallengeService.DAILY_EPOCH);

    assertThatThrownBy(
            () ->
                createDailyGame(
                    service, Optional.of(DailyChallengeService.DAILY_EPOCH.minusDays(1))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void noDateMeansTheServersUtcDate() {
    DailyChallengeService service = service(TODAY);

    assertThat(createDailyGame(service, Optional.empty()).date()).isEqualTo(TODAY);
  }

  @Test
  void theEpochIsNumberOneAndEachDayAddsOne() {
    DailyChallengeService service = service(DailyChallengeService.DAILY_EPOCH);

    assertThat(createDailyGame(service, Optional.empty()).number()).isEqualTo(1);
    assertThat(
            createDailyGame(service, Optional.of(DailyChallengeService.DAILY_EPOCH.plusDays(1)))
                .number())
        .isEqualTo(2);
  }

  @Test
  void generatesRoundsOnlyThroughTheStore() {
    DailyChallengeService service = service(TODAY);
    createDailyGame(service, Optional.empty());

    org.mockito.Mockito.verify(store).findOrCreate(eq(TODAY), anySupplier());
  }

  @Test
  void aDailyGameEnsuresTheDaysRowAndPassesTheDateToTheGame() {
    DailyChallengeService service = service(TODAY);
    createDailyGame(service, Optional.of(TODAY.minusDays(1)));

    org.mockito.Mockito.verify(scores).ensureDay(TODAY.minusDays(1), null);
    org.mockito.Mockito.verify(games).createGame(ROUNDS, "DAILY", null, TODAY.minusDays(1));
  }

  @Test
  void aDayWithNoFinishedGamesHasZerosAndNoAverage() {
    DailyChallengeService service = service(TODAY);
    when(scores.find(TODAY, Difficulty.NORMAL)).thenReturn(DailyScore.empty());

    var response = service.score(TODAY, Difficulty.NORMAL);

    assertThat(response.players()).isZero();
    assertThat(response.averageScore()).isNull();
    assertThat(response.maxScore()).isEqualTo(50);
    assertThat(response.buckets()).hasSize(11).containsOnly(0);
  }

  @Test
  void theAverageIsRoundedToOneDecimalPlace() {
    DailyChallengeService service = service(TODAY);
    List<Integer> buckets = new java.util.ArrayList<>(DailyScore.empty().buckets());
    when(scores.find(TODAY, Difficulty.NORMAL)).thenReturn(new DailyScore(3, 1000, buckets));

    assertThat(service.score(TODAY, Difficulty.NORMAL).averageScore()).isEqualTo(333.3);
  }

  @Test
  void scoreDatesAreValidatedLikeNewGamesExceptThatThePastIsAllowed() {
    DailyChallengeService service = service(TODAY);
    when(scores.find(any(), any())).thenReturn(DailyScore.empty());

    assertThat(service.score(TODAY.plusDays(1), Difficulty.NORMAL).date())
        .isEqualTo(TODAY.plusDays(1));
    assertThat(service.score(DailyChallengeService.DAILY_EPOCH, Difficulty.NORMAL).date())
        .isEqualTo(DailyChallengeService.DAILY_EPOCH);
    assertThatThrownBy(() -> service.score(TODAY.plusDays(2), Difficulty.NORMAL))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> service.score(DailyChallengeService.DAILY_EPOCH.minusDays(1), Difficulty.NORMAL))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static Supplier<List<PinnedRound>> anySupplier() {
    return ArgumentMatchers.<Supplier<List<PinnedRound>>>any();
  }

  @Test
  void aStoredChallengeShowsItsTypesInRoundOrderAndNumber() {
    DailyChallengeService service = service(TODAY);
    when(store.find(TODAY))
        .thenReturn(
            Optional.of(
                List.of(
                    new PinnedRound(GameType.LAKES_BY_SHAPE, "lake:a"),
                    new PinnedRound(GameType.TRIPOINT, "tripoint:x"))));

    var challenge = service.challenge(TODAY).orElseThrow();

    assertThat(challenge.date()).isEqualTo(TODAY);
    assertThat(challenge.number()).isEqualTo(4);
    assertThat(challenge.gameTypes()).containsExactly("LAKES_BY_SHAPE", "TRIPOINT");
  }

  @Test
  void aDayWithNoRowIsEmptyAndNothingIsCreated() {
    DailyChallengeService service = service(TODAY);
    when(store.find(TODAY)).thenReturn(Optional.empty());

    assertThat(service.challenge(TODAY)).isEmpty();

    verify(store, never()).findOrCreate(any(), any());
    verifyNoInteractions(scores, games);
  }

  @Test
  void aChallengeDateTwoDaysOutIsRejected() {
    DailyChallengeService service = service(TODAY);

    assertThatThrownBy(() -> service.challenge(TODAY.plusDays(2)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
