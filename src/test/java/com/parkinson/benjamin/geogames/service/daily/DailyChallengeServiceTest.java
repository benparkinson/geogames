package com.parkinson.benjamin.geogames.service.daily;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.api.DailyGameResponse;
import com.parkinson.benjamin.geogames.api.GameCreationResponse;
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
  private final UUID gameId = UUID.randomUUID();

  private DailyChallengeService service(LocalDate utcToday) {
    Clock clock = Clock.fixed(utcToday.atTime(23, 59).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    when(store.findOrCreate(any(), any())).thenReturn(ROUNDS);
    when(games.createGame(ROUNDS, "DAILY")).thenReturn(new GameCreationResponse(gameId));
    return new DailyChallengeService(clock, mock(DailyRoundPicker.class), store, games);
  }

  @Test
  void yesterdayTodayAndTomorrowAreAccepted() {
    DailyChallengeService service = service(TODAY);

    for (int offset = -1; offset <= 1; offset++) {
      LocalDate date = TODAY.plusDays(offset);
      DailyGameResponse response = service.createDailyGame(Optional.of(date));

      assertThat(response.date()).isEqualTo(date);
      assertThat(response.id()).isEqualTo(gameId);
    }
  }

  @Test
  void twoDaysAwayIsRejected() {
    DailyChallengeService service = service(TODAY);

    assertThatThrownBy(() -> service.createDailyGame(Optional.of(TODAY.plusDays(2))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.createDailyGame(Optional.of(TODAY.minusDays(2))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aDateBeforeTheEpochIsRejected() {
    DailyChallengeService service = service(DailyChallengeService.DAILY_EPOCH);

    assertThatThrownBy(
            () ->
                service.createDailyGame(
                    Optional.of(DailyChallengeService.DAILY_EPOCH.minusDays(1))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void noDateMeansTheServersUtcDate() {
    DailyChallengeService service = service(TODAY);

    assertThat(service.createDailyGame(Optional.empty()).date()).isEqualTo(TODAY);
  }

  @Test
  void theEpochIsNumberOneAndEachDayAddsOne() {
    DailyChallengeService service = service(DailyChallengeService.DAILY_EPOCH);

    assertThat(service.createDailyGame(Optional.empty()).number()).isEqualTo(1);
    assertThat(
            service
                .createDailyGame(Optional.of(DailyChallengeService.DAILY_EPOCH.plusDays(1)))
                .number())
        .isEqualTo(2);
  }

  @Test
  void generatesRoundsOnlyThroughTheStore() {
    DailyChallengeService service = service(TODAY);
    service.createDailyGame(Optional.empty());

    org.mockito.Mockito.verify(store).findOrCreate(eq(TODAY), anySupplier());
  }

  private static Supplier<List<PinnedRound>> anySupplier() {
    return ArgumentMatchers.<Supplier<List<PinnedRound>>>any();
  }
}
