package com.parkinson.benjamin.geogames.service.daily;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.Continent;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.RoundContinents;
import com.parkinson.benjamin.geogames.service.RoundGenerator;
import com.parkinson.benjamin.geogames.service.RoundGenerators;
import com.parkinson.benjamin.geogames.service.daily.DailyHistory.PastDay;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DailyRoundPickerTest {

  private static final LocalDate DATE = LocalDate.of(2026, 11, 18);

  private static final List<GameType> POOL =
      Arrays.stream(GameType.values()).filter(type -> !type.extraDifficult()).toList();

  private final List<Set<String>> exclusionsSeen = new ArrayList<>();

  /** A generator over 50 made-up subjects, so every type competes for the same names. */
  private record FakeGenerator(GameType gameType, List<Set<String>> exclusionsSeen)
      implements RoundGenerator {

    @Override
    public List<GameData> generate(int count, Set<String> excludedSubjects, Random random) {
      exclusionsSeen.add(Set.copyOf(excludedSubjects));
      List<GameData> rounds = new ArrayList<>();
      while (rounds.size() < count) {
        String subject = "subject:" + random.nextInt(50);
        if (!excludedSubjects.contains(subject)
            && rounds.stream().noneMatch(r -> r.subjectId().equals(subject))) {
          rounds.add(new FakeRound(gameType, subject));
        }
      }
      return rounds;
    }

    @Override
    public GameData rebuild(String roundKey) {
      return new FakeRound(gameType, roundKey);
    }
  }

  private record FakeRound(GameType gameType, String subjectId) implements GameData {}

  private RoundGenerators fakeGenerators() {
    return new RoundGenerators(
        Arrays.stream(GameType.values())
            .map(type -> (RoundGenerator) new FakeGenerator(type, exclusionsSeen))
            .toList());
  }

  private DailyRoundPicker picker(RoundContinents continents) {
    return new DailyRoundPicker(fakeGenerators(), continents);
  }

  /** Continents that are never worked out, so no day has a penalty. */
  private final DailyRoundPicker picker = picker(mock(RoundContinents.class));

  private static List<PinnedRound> pinned(List<GameData> rounds) {
    return rounds.stream().map(PinnedRound::of).toList();
  }

  private static PastDay day(
      int daysAgo, Set<GameType> types, Set<String> subjects, List<Set<Continent>> continents) {
    return new PastDay(daysAgo, types, subjects, continents);
  }

  private static Set<Continent> in(Continent... continents) {
    return EnumSet.copyOf(Arrays.asList(continents));
  }

  @Test
  void theSameDateGivesTheSameRounds() {
    assertThat(pinned(picker.pick(DATE, DailyHistory.EMPTY)))
        .isEqualTo(pinned(picker.pick(DATE, DailyHistory.EMPTY)));
  }

  @Test
  void theSameDateAndHistoryGiveTheSameRounds() {
    DailyHistory history =
        new DailyHistory(
            List.of(day(1, Set.copyOf(POOL.subList(0, 5)), Set.of("subject:1"), List.of())));

    assertThat(pinned(picker.pick(DATE, history))).isEqualTo(pinned(picker.pick(DATE, history)));
  }

  @Test
  void differentDatesGiveDifferentRounds() {
    Set<List<PinnedRound>> distinct = new HashSet<>();
    for (int day = 0; day < 10; day++) {
      distinct.add(pinned(picker.pick(DATE.plusDays(day), DailyHistory.EMPTY)));
    }

    assertThat(distinct).hasSizeGreaterThan(1);
  }

  @Test
  void everyDayHasFiveDifferentStandardTypesAndFiveDifferentSubjects() {
    for (int day = 0; day < 365; day++) {
      List<GameData> rounds = picker.pick(DATE.plusDays(day), DailyHistory.EMPTY);

      assertThat(rounds).hasSize(DailyRoundPicker.DAILY_ROUNDS);
      assertThat(rounds).extracting(GameData::gameType).doesNotHaveDuplicates();
      assertThat(rounds).extracting(GameData::gameType).noneMatch(GameType::extraDifficult);
      assertThat(rounds).extracting(GameData::subjectId).doesNotHaveDuplicates();
    }
  }

  @Test
  void theTypesThatSatOutYesterdayPlayToday() {
    Set<GameType> yesterday = Set.copyOf(POOL.subList(0, 5));
    DailyHistory history = new DailyHistory(List.of(day(1, yesterday, Set.of(), List.of())));

    for (int d = 0; d < 20; d++) {
      List<GameData> rounds = picker.pick(DATE.plusDays(d), history);

      assertThat(rounds).extracting(GameData::gameType).containsAll(POOL.subList(5, POOL.size()));
    }
  }

  @Test
  void aSubjectUsedSevenDaysAgoIsExcluded() {
    DailyHistory history =
        new DailyHistory(List.of(day(7, Set.of(), Set.of("subject:7"), List.of())));

    List<GameData> rounds = picker.pick(DATE, history);

    assertThat(exclusionsSeen).isNotEmpty().allMatch(excluded -> excluded.contains("subject:7"));
    assertThat(rounds).extracting(GameData::subjectId).doesNotContain("subject:7");
  }

  @Test
  void aSubjectUsedEightDaysAgoIsNotExcluded() {
    DailyHistory history =
        new DailyHistory(List.of(day(8, Set.of(), Set.of("subject:7"), List.of())));

    picker.pick(DATE, history);

    assertThat(exclusionsSeen).isNotEmpty().noneMatch(excluded -> excluded.contains("subject:7"));
  }

  @Test
  void leastRecentlyUsedTypesFillTheRemainingSlots() {
    Set<GameType> yesterday = Set.copyOf(POOL.subList(0, 5));
    Set<GameType> a = Set.of(POOL.get(0), POOL.get(1));
    // A and B were also used on each of the 3 days before; C, D and E were not.
    DailyHistory history =
        new DailyHistory(
            List.of(
                day(1, yesterday, Set.of(), List.of()),
                day(2, a, Set.of(), List.of()),
                day(3, a, Set.of(), List.of()),
                day(4, a, Set.of(), List.of())));

    for (int d = 0; d < 20; d++) {
      assertThat(picker.pick(DATE.plusDays(d), history))
          .extracting(GameData::gameType)
          .doesNotContain(POOL.get(0), POOL.get(1));
    }
  }

  @Test
  void aTypeUsedOnlyFiveDaysAgoCountsAsUnused() {
    DailyHistory history = new DailyHistory(List.of(day(5, Set.copyOf(POOL), Set.of(), List.of())));

    assertThat(history.daysSinceUsed(POOL.get(0), DailyRoundPicker.TYPE_DAYS))
        .isEqualTo(DailyRoundPicker.TYPE_DAYS + 1);
    assertThat(history.recentUses(POOL.get(0), DailyRoundPicker.TYPE_DAYS)).isZero();
  }

  @Test
  void continentPenaltyIsZeroForABalancedDay() {
    List<Set<Continent>> today =
        List.of(
            in(Continent.EUROPE),
            in(Continent.ASIA),
            in(Continent.AFRICA),
            in(Continent.EUROPE),
            in(Continent.OCEANIA));

    assertThat(DailyRoundPicker.continentPenalty(today, DailyHistory.EMPTY)).isZero();
  }

  @Test
  void continentPenaltyIsTwoForAContinentInFourRounds() {
    List<Set<Continent>> today =
        List.of(
            in(Continent.EUROPE),
            in(Continent.EUROPE),
            in(Continent.EUROPE, Continent.ASIA),
            in(Continent.EUROPE),
            in(Continent.AFRICA));

    assertThat(DailyRoundPicker.continentPenalty(today, DailyHistory.EMPTY)).isEqualTo(2);
  }

  @Test
  void continentPenaltyAddsOneWhenTheSameContinentDominatedYesterday() {
    List<Set<Continent>> europeThrice =
        List.of(
            in(Continent.EUROPE),
            in(Continent.EUROPE),
            in(Continent.EUROPE),
            in(Continent.ASIA),
            in(Continent.AFRICA));

    DailyHistory yesterday = new DailyHistory(List.of(day(1, Set.of(), Set.of(), europeThrice)));
    DailyHistory threeDaysAgo = new DailyHistory(List.of(day(3, Set.of(), Set.of(), europeThrice)));

    assertThat(DailyRoundPicker.continentPenalty(europeThrice, yesterday)).isEqualTo(1);
    assertThat(DailyRoundPicker.continentPenalty(europeThrice, threeDaysAgo)).isZero();
  }

  @Test
  void returnsTheFirstAttemptWithNoPenalty() {
    RoundContinents continents = mock(RoundContinents.class);
    AtomicInteger calls = new AtomicInteger();
    // The first two attempts (10 rounds) are all Europe: penalty 2. Later ones are balanced.
    when(continents.of(any()))
        .thenAnswer(
            invocation -> {
              int call = calls.getAndIncrement();
              return call < 10 ? List.of("Europe") : List.of(Continent.values()[call % 6].label());
            });

    List<GameData> rounds = picker(continents).pick(DATE, DailyHistory.EMPTY);

    assertThat(rounds).hasSize(DailyRoundPicker.DAILY_ROUNDS);
    // Two bad attempts, then the third scores 0 and is returned without a fourth.
    verify(continents, times(15)).of(any());
  }

  @Test
  void returnsTheLowestAfterEightAttempts() {
    RoundContinents continents = mock(RoundContinents.class);
    when(continents.of(any())).thenReturn(List.of("Europe"));

    List<GameData> rounds = picker(continents).pick(DATE, DailyHistory.EMPTY);

    assertThat(rounds).hasSize(DailyRoundPicker.DAILY_ROUNDS);
    verify(continents, times(DailyRoundPicker.CONTINENT_ATTEMPTS * DailyRoundPicker.DAILY_ROUNDS))
        .of(any());
  }

  @Test
  void aRoundWhoseContinentsCantBeWorkedOutCountsAsHavingNone() {
    RoundContinents continents = mock(RoundContinents.class);
    when(continents.of(any())).thenThrow(new IllegalStateException("no continents"));

    assertThat(picker(continents).pick(DATE, DailyHistory.EMPTY))
        .hasSize(DailyRoundPicker.DAILY_ROUNDS);
  }

  @Test
  void neighbouringDatesHaveDifferentSeeds() {
    assertThat(DailyRoundPicker.seedFor(DATE))
        .isNotEqualTo(DailyRoundPicker.seedFor(DATE.plusDays(1)));
  }

  @Test
  void failsAtStartupWithoutEnoughStandardTypes() {
    assertThatThrownBy(
            () ->
                new DailyRoundPicker(
                    new RoundGenerators(
                        List.of(new FakeGenerator(GameType.TRIPOINT, new ArrayList<>()))),
                    mock(RoundContinents.class)))
        .isInstanceOf(IllegalStateException.class);
  }
}
