package com.parkinson.benjamin.geogames.loaders.features;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.loaders.features.FeatureSelector.Selection;
import com.parkinson.benjamin.geogames.loaders.rivers.RiverAssembler;
import com.parkinson.benjamin.geogames.loaders.rivers.RiverAssembler.Assembly;
import com.parkinson.benjamin.geogames.loaders.rivers.RiverCandidate;
import com.parkinson.benjamin.geogames.loaders.rivers.RiverOverride;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FeatureSelectorTest {

  /** About 333 km: long enough to pass the 200 km filter. */
  private static final List<List<double[]>> LONG_LINES =
      List.of(List.of(new double[] {0, 0}, new double[] {0, 3}));

  private static NaturalEarthFeature group(String qid, String name) {
    return new NaturalEarthFeature(qid, name, name, LONG_LINES, List.of());
  }

  private static WikidataEntry entry(String label, int sitelinks, String continent) {
    return new WikidataEntry(label, sitelinks, List.of(), List.of(continent), List.of());
  }

  private static RiverOverride override(boolean exclude, String name, String continent) {
    return new RiverOverride(exclude, name, continent, null, null, null, null, null, null, null);
  }

  private static Assembly assemble(
      Map<String, NaturalEarthFeature> groups,
      Map<String, WikidataEntry> wikidata,
      Map<String, RiverOverride> overrides) {
    return new RiverAssembler(file -> LONG_LINES).assemble(groups, wikidata, overrides);
  }

  @Test
  void twoFeaturesWithTheSameQidBecomeOneRiver() {
    String json =
        """
        {"features": [
          {"properties": {"wikidataid": "Q1", "name": "", "name_en": "Rhine"},
           "geometry": {"type": "LineString", "coordinates": [[0,0],[0,1]]}},
          {"properties": {"wikidataid": "Q1", "name": "Rhein", "name_en": "Rhine"},
           "geometry": {"type": "MultiLineString", "coordinates": [[[0,1],[0,2]],[[1,1],[1,2]]]}},
          {"properties": {"wikidataid": null, "name": "Nameless"},
           "geometry": {"type": "LineString", "coordinates": [[5,5],[6,6]]}},
          {"properties": {"wikidataid": "Q9", "featurecla": "Canal", "name": "Canal"},
           "geometry": {"type": "LineString", "coordinates": [[5,5],[6,6]]}}
        ]}
        """;

    Map<String, NaturalEarthFeature> groups =
        new NaturalEarthReader().group(GeoJson.lenientReader().readTree(json), Set.of("Canal"));

    assertThat(groups).containsOnlyKeys("Q1");
    assertThat(groups.get("Q1").lines()).hasSize(3);
    assertThat(groups.get("Q1").name()).isEqualTo("Rhein");
    assertThat(groups.get("Q1").nameEn()).isEqualTo("Rhine");
  }

  @Test
  void eachContinentTakesItsTopRiversBySitelinks() {
    Map<String, NaturalEarthFeature> groups =
        Map.of(
            "Q1", group("Q1", "A"),
            "Q2", group("Q2", "B"),
            "Q3", group("Q3", "C"),
            "Q4", group("Q4", "D"),
            "Q5", group("Q5", "E"));
    Map<String, WikidataEntry> wikidata =
        Map.of(
            "Q1", entry("A", 10, "Asia"),
            "Q2", entry("B", 30, "Asia"),
            "Q3", entry("C", 20, "Asia"),
            "Q4", entry("D", 5, "Africa"),
            "Q5", entry("E", 1, "Europe"));

    Selection<RiverCandidate> selection =
        FeatureSelector.select(
            assemble(groups, wikidata, Map.of()).candidates(), Map.of("Asia", 2, "Africa", 1));

    assertThat(selection.selected())
        .extracting(RiverCandidate::name)
        .containsExactlyInAnyOrder("B", "C", "D");
    assertThat(selection.beyondLimit()).isEqualTo(2);
  }

  @Test
  void overrideRiversCompeteOnSitelinksAndExcludedOnesNeverAppear() {
    Map<String, NaturalEarthFeature> groups =
        Map.of(
            "Q1", group("Q1", "Famous"), "Q2", group("Q2", "Banned"), "Q3", group("Q3", "Obscure"));
    Map<String, WikidataEntry> wikidata =
        Map.of(
            "Q1", entry("Famous", 100, "Asia"),
            "Q2", entry("Banned", 500, "Asia"),
            "Q3", entry("Obscure", 1, "Asia"));
    Map<String, RiverOverride> overrides =
        Map.of("Q2", override(true, null, null), "Q3", override(false, "Renamed", null));

    Selection<RiverCandidate> selection =
        FeatureSelector.select(
            assemble(groups, wikidata, overrides).candidates(), Map.of("Asia", 1));

    assertThat(selection.selected()).extracting(RiverCandidate::name).containsExactly("Famous");
    assertThat(selection.beyondLimit()).isEqualTo(1);
  }

  @Test
  void anOverrideWithOnlyAnOsmFileNeedsNothingElse() {
    Map<String, RiverOverride> overrides =
        Map.of(
            "Q7",
            new RiverOverride(
                null,
                "Murray River",
                "Australia",
                List.of("Murray"),
                null,
                null,
                "m.json",
                null,
                null,
                null));

    Assembly assembly = assemble(Map.of(), Map.of(), overrides);

    assertThat(assembly.candidates())
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.name()).isEqualTo("Murray River");
              assertThat(c.continent()).isEqualTo("Australia");
              assertThat(c.aliases()).containsExactly("Murray");
            });
  }

  @Test
  void shortNonOverrideRiversAreDroppedAndUnresolvedOnesAreSkippedAndRecorded() {
    NaturalEarthFeature tiny =
        new NaturalEarthFeature(
            "Q1",
            "Tiny",
            "Tiny",
            List.of(List.of(new double[] {0, 0}, new double[] {0, 0.5})),
            List.of());
    Map<String, NaturalEarthFeature> groups = Map.of("Q1", tiny, "Q2", group("Q2", "Lost"));
    Map<String, WikidataEntry> wikidata =
        Map.of(
            "Q1",
            entry("Tiny", 99, "Asia"),
            "Q2",
            new WikidataEntry("Lost", 50, List.of(), List.of("Europe", "Asia"), List.of()));

    Assembly assembly = assemble(groups, wikidata, Map.of());
    Selection<RiverCandidate> selection =
        FeatureSelector.select(assembly.candidates(), Map.of("Asia", 10));

    assertThat(assembly.dropped()).containsEntry("shorter than 200 km", 1);
    assertThat(selection.selected()).isEmpty();
    assertThat(selection.unresolved()).extracting(RiverCandidate::name).containsExactly("Lost");
  }

  @Test
  void slugStripsAccentsAndPunctuation() {
    assertThat(Slugs.slug("Rhône", "river")).isEqualTo("rhone");
    assertThat(Slugs.slug("Rio Grande", "river")).isEqualTo("rio-grande");
    assertThat(Slugs.slug("São Francisco River!", "river")).isEqualTo("sao-francisco-river");
  }

  @Test
  void polygonsAreGroupedByQidWithTheirRings() {
    String json =
        """
        {"features": [
          {"properties": {"wikidataid": "Q5", "name": "Lake", "name_en": "Lake"},
           "geometry": {"type": "Polygon", "coordinates":
             [[[0,0],[4,0],[4,4],[0,4],[0,0]], [[1,1],[2,1],[2,2],[1,1]]]}},
          {"properties": {"wikidataid": "Q5", "name": "Lake", "name_en": "Lake"},
           "geometry": {"type": "MultiPolygon", "coordinates":
             [[[[10,10],[11,10],[11,11],[10,10]]], [[[20,20],[21,20],[21,21],[20,20]]]]}},
          {"properties": {"wikidataid": "Q6", "featurecla": "Reservoir", "name": "Skipped"},
           "geometry": {"type": "Polygon", "coordinates": [[[0,0],[1,0],[1,1],[0,0]]]}}
        ]}
        """;

    Map<String, NaturalEarthFeature> groups =
        new NaturalEarthReader().group(GeoJson.lenientReader().readTree(json), Set.of("Reservoir"));

    assertThat(groups).containsOnlyKeys("Q5");
    assertThat(groups.get("Q5").lines()).isEmpty();
    assertThat(groups.get("Q5").polygons()).hasSize(3);
    assertThat(groups.get("Q5").polygons().get(0)).hasSize(2);
    assertThat(groups.get("Q5").polygons().get(0).get(0)).hasSize(5);
    assertThat(groups.get("Q5").polygons().get(2).get(0).get(0)).containsExactly(20, 20);
  }

  @Test
  void locateRingsUsesTheOuterRingsOfPolygons() {
    CountryLocator locator =
        new CountryLocator(
            List.of(
                country("West", List.of(ring(-2, -2, 0, 2))),
                country("East", List.of(ring(0, -2, 2, 2)))),
            5);
    List<double[]> lake =
        List.of(
            new double[] {-1, -1},
            new double[] {1, -1},
            new double[] {1, 1},
            new double[] {-1, 1},
            new double[] {-1, -1});

    CountryLocator.Result result = locator.locateRings(List.of(List.of(lake)));

    assertThat(result.countries()).containsExactlyInAnyOrder("West", "East");
  }

  private static List<List<Double>> ring(
      double minLon, double minLat, double maxLon, double maxLat) {
    return List.of(
        List.of(minLon, minLat),
        List.of(maxLon, minLat),
        List.of(maxLon, maxLat),
        List.of(minLon, maxLat),
        List.of(minLon, minLat));
  }

  private static com.parkinson.benjamin.geogames.model.Country country(String name, List<?> rings) {
    return new com.parkinson.benjamin.geogames.model.Country(
        name,
        List.of(),
        new com.parkinson.benjamin.geogames.model.geojson.GeoData(
            "Feature",
            new com.parkinson.benjamin.geogames.model.geojson.GeoProperties(name),
            new com.parkinson.benjamin.geogames.model.geojson.GeoGeometry("Polygon", rings)));
  }
}
