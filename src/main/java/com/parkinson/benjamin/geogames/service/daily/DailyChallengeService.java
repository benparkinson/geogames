package com.parkinson.benjamin.geogames.service.daily;

import com.parkinson.benjamin.geogames.api.DailyGameResponse;
import com.parkinson.benjamin.geogames.service.GameService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Makes a game from a date's pinned rounds. Each call makes a new game (answers are stored per
 * game); the rounds are the same for everyone on that date.
 *
 * <p>Not {@code @Transactional}: {@link DailyChallengeStore} must commit its own insert, and the
 * game is created in its own transaction by {@link GameService#createGame(List, String)}.
 */
@Service
public class DailyChallengeService {

  /** The date of Daily #1. */
  public static final LocalDate DAILY_EPOCH = LocalDate.of(2026, 11, 17);

  public static final String GAME_MODE = "DAILY";

  private final Clock clock;
  private final DailyRoundPicker picker;
  private final DailyChallengeStore store;
  private final GameService gameService;

  public DailyChallengeService(
      Clock clock, DailyRoundPicker picker, DailyChallengeStore store, GameService gameService) {
    this.clock = clock;
    this.picker = picker;
    this.store = store;
    this.gameService = gameService;
  }

  /**
   * @param requested the player's local date, or empty for the server's UTC date. Accepted within
   *     one day of the server's UTC date, which covers every time zone (UTC-12 to UTC+14).
   * @throws IllegalArgumentException if the date is out of range
   */
  public DailyGameResponse createDailyGame(Optional<LocalDate> requested) {
    LocalDate today = LocalDate.now(clock);
    LocalDate date = requested.orElse(today);
    if (Math.abs(ChronoUnit.DAYS.between(today, date)) > 1) {
      throw new IllegalArgumentException("That date is too far from today: " + date);
    }
    if (date.isBefore(DAILY_EPOCH)) {
      throw new IllegalArgumentException("There is no daily challenge before " + DAILY_EPOCH);
    }
    List<PinnedRound> rounds =
        store.findOrCreate(date, () -> picker.pick(date).stream().map(PinnedRound::of).toList());
    UUID id = gameService.createGame(rounds, GAME_MODE).id();
    return new DailyGameResponse(id, date, number(date));
  }

  static long number(LocalDate date) {
    return ChronoUnit.DAYS.between(DAILY_EPOCH, date) + 1;
  }
}
