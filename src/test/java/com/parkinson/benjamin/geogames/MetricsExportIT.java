package com.parkinson.benjamin.geogames;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * With export turned on, the app really pushes metrics over OTLP, with the configured header. The
 * receiver is a local HTTP server, so nothing leaves the machine.
 */
@SpringBootTest
class MetricsExportIT {

  private static final BlockingQueue<String> AUTHORIZATIONS = new LinkedBlockingQueue<>();
  private static final HttpServer RECEIVER = startReceiver();

  @DynamicPropertySource
  static void exportToTheReceiver(DynamicPropertyRegistry registry) {
    registry.add("management.otlp.metrics.export.enabled", () -> "true");
    registry.add(
        "management.otlp.metrics.export.url",
        () -> "http://localhost:%d/otlp/v1/metrics".formatted(RECEIVER.getAddress().getPort()));
    registry.add("management.otlp.metrics.export.headers.Authorization", () -> "Basic dGVzdA==");
    registry.add("management.otlp.metrics.export.step", () -> "1s");
  }

  @AfterAll
  static void stopReceiver() {
    RECEIVER.stop(0);
  }

  @Test
  void metricsArePushedWithTheConfiguredHeader() throws InterruptedException {
    assertThat(AUTHORIZATIONS.poll(30, TimeUnit.SECONDS)).isEqualTo("Basic dGVzdA==");
  }

  private static HttpServer startReceiver() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      server.createContext(
          "/otlp/v1/metrics",
          exchange -> {
            exchange.getRequestBody().readAllBytes();
            AUTHORIZATIONS.add(
                String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
          });
      server.start();
      return server;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
