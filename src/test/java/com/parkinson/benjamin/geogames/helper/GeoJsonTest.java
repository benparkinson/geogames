package com.parkinson.benjamin.geogames.helper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeoJsonTest {

  record Sample(String zebra, String apple) {}

  @Test
  void lenientReaderIgnoresPropertiesTheTypeDoesNotHave() {
    Sample sample =
        GeoJson.lenientReader()
            .readValue("{\"zebra\":\"z\",\"apple\":\"a\",\"extra\":1}", Sample.class);

    assertThat(sample).isEqualTo(new Sample("z", "a"));
  }

  @Test
  void prettyWriterIndentsAndKeepsDeclarationOrder() {
    String json = GeoJson.prettyWriter().writeValueAsString(new Sample("z", "a"));

    assertThat(json).contains("\n");
    assertThat(json.indexOf("zebra")).isLessThan(json.indexOf("apple"));
    assertThat(json.replaceAll("\\s", "")).isEqualTo("{\"zebra\":\"z\",\"apple\":\"a\"}");
  }

  @Test
  void whatTheWriterWritesTheReaderReadsBack() {
    Sample original = new Sample("z", "a");

    String json = GeoJson.prettyWriter().writeValueAsString(original);

    assertThat(GeoJson.lenientReader().readValue(json, Sample.class)).isEqualTo(original);
  }
}
