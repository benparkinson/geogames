package com.parkinson.benjamin.geogames.loaders.features;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WikidataMainTest {

  private static final String RESULT =
      """
      {"results": {"bindings": [
        {
          "item": {"type": "uri", "value": "http://www.wikidata.org/entity/Q2"},
          "label": {"type": "literal", "value": "Danube"},
          "sitelinks": {"type": "literal", "value": "120"},
          "aliases": {"type": "literal", "value": "Donau|Dunav"},
          "continents": {"type": "literal", "value": "Europe"},
          "countryContinents": {"type": "literal", "value": "Europe|Asia"}
        },
        {
          "item": {"type": "uri", "value": "http://www.wikidata.org/entity/Q1"}
        }
      ]}}
      """;

  @Test
  void buildQueryListsTheQidsInOrderInTheValuesClause() {
    String query = WikidataMain.buildQuery(List.of("Q3", "Q1", "Q2"));

    assertThat(query).contains("VALUES ?item { wd:Q3 wd:Q1 wd:Q2 }");
  }

  @Test
  void buildQueryWithNoQidsHasAnEmptyValuesClause() {
    assertThat(WikidataMain.buildQuery(List.of())).contains("VALUES ?item { }");
  }

  @Test
  void buildQueryRejectsAnythingThatIsNotAQid() {
    assertThatThrownBy(() -> WikidataMain.buildQuery(List.of("Q1", "Q2} DROP")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Q2} DROP");
  }

  @Test
  void parseReadsEveryFieldOfAFullRow() {
    Map<String, WikidataEntry> parsed = WikidataMain.parse(RESULT);

    assertThat(parsed.get("Q2"))
        .isEqualTo(
            new WikidataEntry(
                "Danube",
                120,
                List.of("Donau", "Dunav"),
                List.of("Europe"),
                List.of("Europe", "Asia")));
  }

  @Test
  void parseTreatsMissingOptionalFieldsAsEmpty() {
    Map<String, WikidataEntry> parsed = WikidataMain.parse(RESULT);

    assertThat(parsed.get("Q1"))
        .isEqualTo(new WikidataEntry("", 0, List.of(), List.of(), List.of()));
  }

  @Test
  void parseKeysTheResultByQidInSortedOrder() {
    assertThat(WikidataMain.parse(RESULT).keySet()).containsExactly("Q1", "Q2");
  }

  @Test
  void parseOfNoBindingsIsEmpty() {
    assertThat(WikidataMain.parse("{\"results\": {\"bindings\": []}}")).isEmpty();
    assertThat(WikidataMain.parse("{}")).isEmpty();
  }

  @Test
  void parseTakesWhateverFollowsTheLastSlashAsTheQid() {
    String json =
        """
        {"results": {"bindings": [{"item": {"value": "not-a-uri"}}]}}
        """;

    // Current behaviour: a malformed item is not rejected. With no slash the whole value becomes
    // the key, where the loaders' QID pattern would never match it.
    assertThat(WikidataMain.parse(json)).containsOnlyKeys("not-a-uri");
  }

  @Test
  void parseFailsOnANonNumericSitelinkCount() {
    String json =
        """
        {"results": {"bindings": [{
          "item": {"value": "http://www.wikidata.org/entity/Q1"},
          "sitelinks": {"value": "many"}
        }]}}
        """;

    assertThatThrownBy(() -> WikidataMain.parse(json)).isInstanceOf(NumberFormatException.class);
  }
}
