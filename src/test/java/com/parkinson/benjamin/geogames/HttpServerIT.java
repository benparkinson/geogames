package com.parkinson.benjamin.geogames;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Starts the real server (Tomcat on a random port) and calls it over HTTP, which the MockMvc tests
 * never do, including the static site the jar serves.
 *
 * <p>The static-site checks only mean something when the frontend was built into the classpath
 * ({@code public/index.html}, copied from {@code client/out}), so the whole class is skipped in a
 * backend-only build on a developer machine. In CI (the {@code CI} variable is set) it is never
 * skipped: the integration job always builds the frontend first, and if the site is missing there
 * the static-site test fails the build instead of the class hiding.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@EnabledIf("staticSiteIsBuilt")
class HttpServerIT {

  /** https://geogauntlet.onrender.com is the production frontend. */
  private static final String PRODUCTION_ORIGIN = "https://geogauntlet.onrender.com";

  private static final Pattern STATIC_ASSET =
      Pattern.compile("src=\"(/_next/static/[^\"]+\\.js)\"");

  @Autowired private RestTestClient client;

  static boolean staticSiteIsBuilt() {
    // In CI the integration job always has the built site; if it is missing, fail, don't skip.
    return System.getenv("CI") != null || new ClassPathResource("public/index.html").exists();
  }

  @Test
  void healthIsUp() {
    client
        .get()
        .uri("/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Server running!");
  }

  @Test
  void servesAGameOverRealHttpWithCorsForTheProductionFrontend() {
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

    client
        .get()
        .uri("/api/games/{id}/rounds/0", id)
        .header("Origin", PRODUCTION_ORIGIN)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectHeader()
        .valueEquals("Access-Control-Allow-Origin", PRODUCTION_ORIGIN)
        .expectBody()
        .jsonPath("$.gameType.name")
        .isEqualTo("RIVERS_BY_SHAPE");
  }

  @Test
  void servesTheStaticSiteAndItsAssets() {
    String html =
        client
            .get()
            .uri("/")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_HTML)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
    assertThat(html).contains("<div id=\"__next\"");

    Matcher asset = STATIC_ASSET.matcher(html);
    assertThat(asset.find()).as("a /_next/static script is linked from /").isTrue();
    client.get().uri(asset.group(1)).exchange().expectStatus().isOk();
  }

  @Test
  void privacyPageLoadsDirectly() {
    String html =
        client
            .get()
            .uri("/privacy")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
    assertThat(html).contains("Credits and privacy");
  }

  @Test
  void dailyPageLoadsDirectly() {
    String html =
        client
            .get()
            .uri("/daily")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
    assertThat(html).contains("Daily challenge");
  }

  @Test
  void reloadingAGamePageIsA404() {
    // Current behaviour: reloading a game page served by the jar is a 404, because nothing forwards
    // /games/{id} to games/[gameId].html. See the deep-link e2e test, which is marked test.fail().
    client.get().uri("/games/1").exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
  }
}
