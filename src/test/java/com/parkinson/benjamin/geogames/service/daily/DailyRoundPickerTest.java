package com.parkinson.benjamin.geogames.service.daily;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.RoundGenerator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DailyRoundPickerTest {

  /** A generator over 50 made-up subjects, so every type competes for the same names. */
  private record FakeGenerator(GameType gameType) implements RoundGenerator {

    @Override
    public List<GameData> generate(int count, Set<String> excludedSubjects, Random random) {
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

  private final DailyRoundPicker picker =
      new DailyRoundPicker(
          Arrays.stream(GameType.values())
              .map(FakeGenerator::new)
              .map(g -> (RoundGenerator) g)
              .toList());

  private static List<PinnedRound> pinned(List<GameData> rounds) {
    return rounds.stream().map(PinnedRound::of).toList();
  }

  @Test
  void theSameDateGivesTheSameRounds() {
    LocalDate date = LocalDate.of(2026, 11, 18);

    assertThat(pinned(picker.pick(date))).isEqualTo(pinned(picker.pick(date)));
  }

  @Test
  void differentDatesGiveDifferentRounds() {
    Set<List<PinnedRound>> distinct = new HashSet<>();
    for (int day = 0; day < 10; day++) {
      distinct.add(pinned(picker.pick(LocalDate.of(2026, 11, 18).plusDays(day))));
    }

    assertThat(distinct).hasSizeGreaterThan(1);
  }

  @Test
  void everyDayHasFiveDifferentStandardTypesAndFiveDifferentSubjects() {
    LocalDate date = LocalDate.of(2026, 11, 18);
    for (int day = 0; day < 365; day++) {
      List<GameData> rounds = picker.pick(date.plusDays(day));

      assertThat(rounds).hasSize(DailyRoundPicker.DAILY_ROUNDS);
      assertThat(rounds).extracting(GameData::gameType).doesNotHaveDuplicates();
      assertThat(rounds).extracting(GameData::gameType).noneMatch(GameType::extraDifficult);
      assertThat(rounds).extracting(GameData::subjectId).doesNotHaveDuplicates();
    }
  }

  @Test
  void neighbouringDatesHaveDifferentSeeds() {
    assertThat(DailyRoundPicker.seedFor(LocalDate.of(2026, 11, 18)))
        .isNotEqualTo(DailyRoundPicker.seedFor(LocalDate.of(2026, 11, 19)));
  }

  @Test
  void failsAtStartupWithoutEnoughStandardTypes() {
    assertThatThrownBy(() -> new DailyRoundPicker(List.of(new FakeGenerator(GameType.TRIPOINT))))
        .isInstanceOf(IllegalStateException.class);
  }
}
