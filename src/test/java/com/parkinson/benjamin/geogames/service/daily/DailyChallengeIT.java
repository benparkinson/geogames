package com.parkinson.benjamin.geogames.service.daily;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.RoundContinents;
import com.parkinson.benjamin.geogames.service.daily.DailyHistory.PastDay;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The daily challenge through the real API, database and generators. Deliberately not
 * {@code @Transactional}: the store must commit its own rows, as it does in production. Each test
 * uses its own date, within a year of the epoch, so they don't share rows (the picker takes any
 * date; only the service limits the range).
 */
@SpringBootTest
@AutoConfigureMockMvc
class DailyChallengeIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private DailyChallengeStore store;
  @Autowired private DailyRoundPicker picker;
  @Autowired private RoundContinents roundContinents;

  private JsonNode createDaily(String body) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(post("/api/daily-games").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  private List<String> roundBlobs(String gameId, int rounds) throws Exception {
    List<String> blobs = new ArrayList<>();
    for (int i = 0; i < rounds; i++) {
      String json =
          mockMvc
              .perform(get("/api/games/%s/rounds/%d".formatted(gameId, i)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();
      blobs.add(objectMapper.readTree(json).get("jsonBlob").asString());
    }
    return blobs;
  }

  @Test
  void twoGamesForTheSameDateAreDifferentGamesWithIdenticalRounds() throws Exception {
    LocalDate date = LocalDate.now(java.time.ZoneOffset.UTC);
    String body = "{\"date\":\"%s\"}".formatted(date);

    JsonNode first = createDaily(body);
    JsonNode second = createDaily(body);

    assertThat(first.get("id").asString()).isNotEqualTo(second.get("id").asString());
    assertThat(first.get("date").asString()).isEqualTo(date.toString());
    assertThat(first.get("number").asLong()).isEqualTo(second.get("number").asLong());
    assertThat(roundBlobs(first.get("id").asString(), 5))
        .isEqualTo(roundBlobs(second.get("id").asString(), 5));
  }

  @Test
  void aStoredRowIsUsedEvenIfThePickerWouldNowChooseDifferently() throws Exception {
    LocalDate tomorrow = LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1);
    List<GameData> others = picker.pick(tomorrow.plusDays(40), DailyHistory.EMPTY);
    String json = objectMapper.writeValueAsString(others.stream().map(PinnedRound::of).toList());
    jdbc.update("delete from daily_challenge where challenge_date = ?", tomorrow);
    jdbc.update(
        "insert into daily_challenge (challenge_date, rounds) values (?, ?)", tomorrow, json);

    JsonNode game = createDaily("{\"date\":\"%s\"}".formatted(tomorrow));

    assertThat(game.get("date").asString()).isEqualTo(tomorrow.toString());
    assertThat(store.find(tomorrow)).contains(others.stream().map(PinnedRound::of).toList());
    assertThat(roundBlobs(game.get("id").asString(), 5)).hasSize(5);
    jdbc.update("delete from daily_challenge where challenge_date = ?", tomorrow);
  }

  @Test
  void twoThreadsCreatingTheSameNewDateGetTheSameRoundsAndOneRow() throws Exception {
    LocalDate date = LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1);
    jdbc.update("delete from daily_challenge where challenge_date = ?", date);
    CountDownLatch go = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Callable<List<PinnedRound>> task =
          () -> {
            go.await();
            return store.findOrCreate(
                date,
                () -> picker.pick(date, DailyHistory.EMPTY).stream().map(PinnedRound::of).toList());
          };
      Future<List<PinnedRound>> a = pool.submit(task);
      Future<List<PinnedRound>> b = pool.submit(task);
      go.countDown();

      assertThat(a.get()).isEqualTo(b.get());
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from daily_challenge where challenge_date = ?",
                  Integer.class,
                  date))
          .isEqualTo(1);
    } finally {
      pool.shutdownNow();
      jdbc.update("delete from daily_challenge where challenge_date = ?", date);
    }
  }

  @Test
  void aMalformedOrOutOfRangeDateIsABadRequest() throws Exception {
    for (String body :
        List.of(
            "{\"date\":\"tomorrow\"}", "{\"date\":\"2001-01-01\"}", "{\"date\":\"2999-01-01\"}")) {
      mockMvc
          .perform(post("/api/daily-games").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }
  }

  @Test
  void findBeforeReturnsOnlyStoredDaysInRangeNewestFirst() {
    LocalDate date = LocalDate.of(2027, 3, 10);
    List<PinnedRound> rounds = List.of(new PinnedRound(GameType.TRIPOINT, "tripoint:x"));
    List<LocalDate> stored =
        List.of(date.minusDays(5), date.minusDays(4), date.minusDays(2), date, date.plusDays(1));
    try {
      for (LocalDate day : stored) {
        store.findOrCreate(day, () -> rounds);
      }

      assertThat(store.findBefore(date, 4).keySet())
          .containsExactly(date.minusDays(2), date.minusDays(4));
    } finally {
      stored.forEach(
          day -> jdbc.update("delete from daily_challenge where challenge_date = ?", day));
    }
  }

  @Test
  void everyDayOfAYearPicksFiveRoundsFromTheRealData() {
    LocalDate start = LocalDate.of(2026, 11, 18);
    List<PastDay> earlier = new ArrayList<>();
    List<List<GameData>> days = new ArrayList<>();
    int penalised = 0;
    int longestRun = 0;
    Map<GameType, Integer> runs = new EnumMap<>(GameType.class);
    for (int day = 0; day < 365; day++) {
      LocalDate date = start.plusDays(day);
      // Nearest first, as DailyHistoryLoader builds it, from the days generated here.
      List<PastDay> recent = new ArrayList<>();
      for (int back = 1; back <= DailyRoundPicker.SUBJECT_DAYS && back <= earlier.size(); back++) {
        PastDay past = earlier.get(earlier.size() - back);
        recent.add(new PastDay(back, past.types(), past.subjects(), past.roundContinents()));
      }
      DailyHistory history = new DailyHistory(recent);

      List<GameData> rounds = picker.pick(date, history);

      assertThat(rounds).hasSize(5);
      assertThat(rounds).extracting(GameData::gameType).doesNotHaveDuplicates();
      assertThat(rounds).extracting(GameData::subjectId).doesNotHaveDuplicates();
      assertThat(rounds).extracting(GameData::gameType).noneMatch(GameType::extraDifficult);
      if (!days.isEmpty()) {
        Set<GameType> yesterday = typesOf(days.get(days.size() - 1));
        assertThat(typesOf(rounds)).as("day %s", date).isNotEqualTo(yesterday);
      }
      if (day >= 1) {
        // No type sits out two days in a row.
        for (GameType type : pool()) {
          boolean sat1 = !typesOf(days.get(days.size() - 1)).contains(type);
          assertThat(sat1 && !typesOf(rounds).contains(type))
              .as("%s sat out two days running on %s", type, date)
              .isFalse();
        }
      }
      Set<String> recentSubjects = new HashSet<>();
      for (int back = 1; back <= DailyRoundPicker.SUBJECT_DAYS && back <= days.size(); back++) {
        days.get(days.size() - back).forEach(r -> recentSubjects.add(r.subjectId()));
      }
      assertThat(rounds.stream().map(GameData::subjectId).filter(recentSubjects::contains))
          .as("subject repeated within 7 days on %s", date)
          .isEmpty();

      for (GameType type : pool()) {
        int run = typesOf(rounds).contains(type) ? runs.merge(type, 1, Integer::sum) : 0;
        runs.put(type, run);
        longestRun = Math.max(longestRun, run);
      }

      if (picker.penaltyOf(rounds, history) > 0) {
        penalised++;
      }
      days.add(rounds);
      earlier.add(
          new PastDay(
              0,
              typesOf(rounds),
              rounds.stream().map(GameData::subjectId).collect(Collectors.toSet()),
              rounds.stream()
                  .map(round -> DailyRoundPicker.continentsOf(roundContinents, round))
                  .toList()));
    }
    System.out.println("Days with a nonzero continent penalty: " + penalised + " of 365");
    System.out.println("Longest run of consecutive days one type played: " + longestRun);
  }

  private static List<GameType> pool() {
    return Arrays.stream(GameType.values()).filter(type -> !type.extraDifficult()).toList();
  }

  private static Set<GameType> typesOf(List<GameData> rounds) {
    return rounds.stream().map(GameData::gameType).collect(Collectors.toSet());
  }
}
