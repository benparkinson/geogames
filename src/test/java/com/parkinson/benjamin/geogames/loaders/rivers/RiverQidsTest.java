package com.parkinson.benjamin.geogames.loaders.rivers;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.loaders.features.NaturalEarthReader;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class RiverQidsTest {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private static String feature(String cla, int rivernum, String name, String qid) {
    return "{\"properties\":{\"featurecla\":\""
        + cla
        + "\",\"rivernum\":"
        + rivernum
        + ",\"name\":\""
        + name
        + "\",\"wikidataid\":\""
        + qid
        + "\"}}";
  }

  private static JsonNode collection(String... features) {
    return MAPPER.readTree("{\"features\":[" + String.join(",", features) + "]}");
  }

  private static String qid(JsonNode collection, int index) {
    return NaturalEarthReader.wikidataId(collection.path("features").get(index).path("properties"));
  }

  private static RiverOverride unlink(String... names) {
    return new RiverOverride(
        null, null, null, null, null, null, null, List.of(names), null, null, null);
  }

  @Test
  void aNameInUnlinkNamesLosesItsQidAndOtherNamesKeepIt() {
    JsonNode tree =
        collection(
            feature("River", 1, "Guainía", "Q1"),
            feature("River", 2, "Negro", "Q1"),
            feature("River", 3, "Guainía", "Q2"));

    RiverQids.correct(tree, Map.of("Q1", unlink("Guainía")));

    assertThat(qid(tree, 0)).isEmpty();
    assertThat(qid(tree, 1)).isEqualTo("Q1");
    assertThat(qid(tree, 2)).isEqualTo("Q2");
  }

  @Test
  void aQidLessLakeCenterlineAdoptsTheQidOfTheRiverWithTheSameRivernumAndName() {
    JsonNode tree =
        collection(
            feature("River", 66, "Huang", "Q7"), feature("Lake Centerline", 66, "Huang", ""));

    RiverQids.correct(tree, Map.of());

    assertThat(qid(tree, 1)).isEqualTo("Q7");
  }

  @Test
  void aLakeCenterlineIsLeftAloneWhenTheNameDiffersOrItHasAQidOrTwoQidsShareTheKey() {
    JsonNode tree =
        collection(
            feature("River", 1, "A", "Q1"),
            feature("Lake Centerline", 1, "B", ""),
            feature("Lake Centerline", 1, "A", "Q9"),
            feature("River", 2, "C", "Q2"),
            feature("River", 2, "C", "Q3"),
            feature("Lake Centerline", 2, "C", ""));

    RiverQids.correct(tree, Map.of());

    assertThat(qid(tree, 1)).isEmpty();
    assertThat(qid(tree, 2)).isEqualTo("Q9");
    assertThat(qid(tree, 5)).isEmpty();
  }

  @Test
  void aFeatureThatIsNotALakeCenterlineIsNeverAdopted() {
    JsonNode tree = collection(feature("River", 1, "A", "Q1"), feature("River", 1, "A", ""));

    RiverQids.correct(tree, Map.of());

    assertThat(qid(tree, 1)).isEmpty();
  }
}
