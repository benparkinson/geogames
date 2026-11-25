package com.parkinson.benjamin.geogames;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/** Real HTTP requests are timed with templated URIs only, and nothing is exported or exposed. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class MetricsIT {

  @Autowired private RestTestClient client;
  @Autowired private MeterRegistry registry;
  @Autowired private ApplicationContext context;

  @Test
  void requestsAreTimedByTemplatedUriWithoutIds() {
    byte[] created =
        client
            .post()
            .uri("/api/games")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"gameType\":\"RIVERS_BY_SHAPE\",\"numberOfRounds\":1}")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .returnResult()
            .getResponseBody();
    String id = JsonPath.read(new String(created), "$.id");
    client.get().uri("/api/games/{id}/rounds/0", id).exchange().expectStatus().isOk();

    assertThat(requests("/api/games", "200")).isNotNull();
    assertThat(requests("/api/games/{gameId}/rounds/{round}", "200")).isNotNull();
    registry
        .find("http.server.requests")
        .timers()
        .forEach(
            timer ->
                assertThat(timer.getId().getTags())
                    .extracting(Tag::getValue)
                    .noneMatch(value -> value.contains(id)));
  }

  @Test
  void gamesAndAnsweredRoundsAreCounted() {
    String id = create("{\"gameType\":\"LAKES_BY_SHAPE\",\"numberOfRounds\":1}");
    create("{}", "/api/daily-games");
    client
        .post()
        .uri("/api/games/{id}/rounds/0/answers", id)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"answerState\":\"CORRECT\",\"hintsUsed\":1,\"wrongGuesses\":2}")
        .exchange()
        .expectStatus()
        .isOk();

    assertThat(sum("geogames.games.started", "mode", "LAKES_BY_SHAPE")).isEqualTo(1);
    assertThat(sum("geogames.games.started", "mode", "DAILY")).isEqualTo(1);
    assertThat(
            registry
                .get("geogames.rounds.answered")
                .tag("answer_state", "CORRECT")
                .tag("hinted", "true")
                .counter()
                .count())
        .isEqualTo(1);
    assertThat(sum("geogames.rounds.hints", "game_type", "LAKES_BY_SHAPE")).isEqualTo(1);
    assertThat(sum("geogames.rounds.wrong_guesses", "game_type", "LAKES_BY_SHAPE")).isEqualTo(2);
  }

  @Test
  void noActuatorEndpointsAreExposed() {
    client.get().uri("/actuator/health").exchange().expectStatus().isNotFound();
    client.get().uri("/actuator/metrics").exchange().expectStatus().isNotFound();
  }

  @Test
  void exportIsOffByDefault() {
    assertThat(context.getBeansOfType(OtlpMeterRegistry.class)).isEmpty();
  }

  private String create(String body) {
    return create(body, "/api/games");
  }

  private String create(String body, String uri) {
    byte[] created =
        client
            .post()
            .uri(uri)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .returnResult()
            .getResponseBody();
    return JsonPath.read(new String(created), "$.id");
  }

  private double sum(String name, String tag, String value) {
    return registry.find(name).tag(tag, value).counters().stream()
        .mapToDouble(c -> c.count())
        .sum();
  }

  private Timer requests(String uri, String status) {
    return registry.find("http.server.requests").tag("uri", uri).tag("status", status).timer();
  }
}
