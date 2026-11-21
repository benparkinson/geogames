package com.parkinson.benjamin.geogames.service.daily;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.Continent;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.RoundContinents;
import com.parkinson.benjamin.geogames.service.RoundDataMissingException;
import com.parkinson.benjamin.geogames.service.RoundGenerator;
import com.parkinson.benjamin.geogames.service.RoundGenerators;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class DailyHistoryLoaderTest {

  private static final LocalDate DATE = LocalDate.of(2026, 11, 20);

  private record FakeRound(GameType gameType, String subjectId) implements GameData {}

  private record FakeGenerator(GameType gameType) implements RoundGenerator {
    @Override
    public List<GameData> generate(int count, Set<String> excluded, Random random) {
      throw new UnsupportedOperationException();
    }

    @Override
    public GameData rebuild(String roundKey) {
      if (roundKey.startsWith("gone")) {
        throw new RoundDataMissingException(gameType, roundKey);
      }
      return new FakeRound(gameType, roundKey);
    }
  }

  @Test
  void aRoundThatNoLongerRebuildsIsSkippedButItsTypeIsKept() {
    DailyChallengeStore store = mock(DailyChallengeStore.class);
    TreeMap<LocalDate, List<PinnedRound>> stored = new TreeMap<>(Comparator.reverseOrder());
    stored.put(
        DATE.minusDays(1),
        List.of(
            new PinnedRound(GameType.TRIPOINT, "gone:1"),
            new PinnedRound(GameType.LAKES_BY_SHAPE, "lake:2")));
    when(store.findBefore(DATE, DailyHistoryLoader.LOOKBACK_DAYS)).thenReturn(stored);
    RoundContinents continents = mock(RoundContinents.class);
    when(continents.of(any())).thenReturn(List.of("Europe"));
    RoundGenerators generators =
        new RoundGenerators(
            Arrays.stream(GameType.values())
                .map(type -> (RoundGenerator) new FakeGenerator(type))
                .toList());

    DailyHistory history = new DailyHistoryLoader(store, generators, continents).load(DATE);

    assertThat(history.days()).hasSize(1);
    DailyHistory.PastDay day = history.days().get(0);
    assertThat(day.daysAgo()).isEqualTo(1);
    assertThat(day.types()).containsExactlyInAnyOrder(GameType.TRIPOINT, GameType.LAKES_BY_SHAPE);
    assertThat(day.subjects()).containsExactly("lake:2");
    assertThat(day.roundContinents()).containsExactly(Set.of(Continent.EUROPE));
  }

  @Test
  void noStoredDaysGiveAnEmptyHistory() {
    DailyChallengeStore store = mock(DailyChallengeStore.class);
    when(store.findBefore(any(), org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(new TreeMap<>());

    DailyHistory history =
        new DailyHistoryLoader(store, mock(RoundGenerators.class), mock(RoundContinents.class))
            .load(DATE);

    assertThat(history.days()).isEmpty();
  }
}
