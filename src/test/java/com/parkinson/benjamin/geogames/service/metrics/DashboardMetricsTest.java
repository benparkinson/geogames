package com.parkinson.benjamin.geogames.service.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Keeps the committed Grafana dashboard in step with {@link GameMetrics}. */
class DashboardMetricsTest {

  private static final Pattern METRIC = Pattern.compile("\\bgeogames_\\w+");
  private static final Pattern SELECTOR = Pattern.compile("\\b(geogames_\\w+)\\{([^}]*)}");
  private static final Pattern LABEL = Pattern.compile("(\\w+)\\s*(?:=~|!~|!=|=)\\s*\"[^\"]*\"");

  private final JsonNode dashboard =
      JsonMapper.shared().readTree(Path.of("grafana/geoggernaut.dashboard.json"));

  /** PromQL name to label keys. */
  private final Map<String, Set<String>> metrics = registeredMetrics();

  @Test
  void everyQueriedMetricExists() {
    List<String> queried = new ArrayList<>();
    for (String expr : expressions()) {
      METRIC.matcher(expr).results().forEach(match -> queried.add(match.group()));
    }

    assertThat(queried).isNotEmpty();
    assertThat(metrics.keySet()).containsAll(queried);
  }

  @Test
  void everyFilteredLabelExists() {
    for (String expr : expressions()) {
      Matcher selector = SELECTOR.matcher(expr);
      while (selector.find()) {
        String metric = selector.group(1);
        List<String> labels =
            LABEL.matcher(selector.group(2)).results().map(match -> match.group(1)).toList();
        assertThat(metrics.getOrDefault(metric, Set.of()))
            .as("labels of %s in %s", metric, expr)
            .containsAll(labels);
      }
    }
  }

  @Test
  void dashboardIsPortable() {
    List<String> dataSources =
        dashboard.findValues("datasource").stream()
            .map(source -> source.path("uid").asString(""))
            .toList();

    assertThat(dataSources).isNotEmpty().isSubsetOf("${DS_PROMETHEUS}", "-- Grafana --", "grafana");
    assertThat(dashboard.path("__inputs").findValuesAsString("name"))
        .containsExactly("DS_PROMETHEUS");
    assertThat(dashboard.path("id").isMissingNode() || dashboard.path("id").isNull()).isTrue();
  }

  /** Every panel's query, however deeply the panel is nested. */
  private List<String> expressions() {
    return dashboard.findValuesAsString("expr");
  }

  private static Map<String, Set<String>> registeredMetrics() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    new GameMetrics(registry);
    Map<String, Set<String>> names = new HashMap<>();
    for (Meter meter : registry.getMeters()) {
      String name = meter.getId().getName().replace('.', '_');
      names.put(
          meter instanceof Counter ? name + "_total" : name,
          meter.getId().getTags().stream().map(Tag::getKey).collect(Collectors.toSet()));
    }
    return names;
  }
}
