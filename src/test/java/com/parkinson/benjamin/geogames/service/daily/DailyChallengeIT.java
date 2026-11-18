package com.parkinson.benjamin.geogames.service.daily;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.model.GameData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    List<GameData> others = picker.pick(tomorrow.plusDays(40));
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
                date, () -> picker.pick(date).stream().map(PinnedRound::of).toList());
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
  void everyDayOfAYearPicksFiveRoundsFromTheRealData() {
    LocalDate start = LocalDate.of(2026, 11, 18);
    for (int day = 0; day < 365; day++) {
      List<GameData> rounds = picker.pick(start.plusDays(day));

      assertThat(rounds).hasSize(5);
      assertThat(rounds).extracting(GameData::gameType).doesNotHaveDuplicates();
      assertThat(rounds).extracting(GameData::subjectId).doesNotHaveDuplicates();
    }
  }
}
