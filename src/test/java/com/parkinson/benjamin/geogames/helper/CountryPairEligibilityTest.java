package com.parkinson.benjamin.geogames.helper;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.Bounds;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.CountryShape.Neighbour;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class CountryPairEligibilityTest {

  private static GeoData data(String type) {
    return new GeoData("Feature", new GeoProperties("x"), new GeoGeometry(type, List.of()));
  }

  private static CountryShape shape(String name, long areaKm2, Neighbour... neighbours) {
    return new CountryShape(
        name,
        List.of(),
        100,
        100,
        areaKm2,
        List.of(neighbours),
        new Bounds(0, 0, 1, 1),
        data("Polygon"),
        data("MultiLineString"),
        data("MultiLineString"));
  }

  private static Neighbour neighbour(String name, double km, boolean mainland) {
    return new Neighbour(name, km, mainland, List.of(0));
  }

  private final CountryShape a = shape("A", 1000, neighbour("B", 200, true));
  private final CountryShape b = shape("B", 2000, neighbour("A", 200, true));

  @Test
  void aLongMainlandBorderIsEligibleForBothGames() {
    assertThat(CountryPairEligibility.borderShapes().test(a, b)).isTrue();
    assertThat(CountryPairEligibility.mergedCountries().test(a, b)).isTrue();
  }

  @Test
  void countriesThatDontBorderAreNotEligible() {
    CountryShape c = shape("C", 1000);

    assertThat(CountryPairEligibility.borderShapes().test(a, c)).isFalse();
    assertThat(CountryPairEligibility.mergedCountries().test(a, c)).isFalse();
  }

  @Test
  void aBorderThatOnlyOneSideListsIsNotEligible() {
    CountryShape oneSided = shape("B", 2000);

    assertThat(CountryPairEligibility.borderShapes().test(a, oneSided)).isFalse();
  }

  @Test
  void aBorderInAnOverseasPartIsNotEligible() {
    CountryShape overseas = shape("A", 1000, neighbour("B", 200, false));

    assertThat(CountryPairEligibility.borderShapes().test(overseas, b)).isFalse();
    assertThat(
            CountryPairEligibility.borderShapes()
                .test(a, shape("B", 2000, neighbour("A", 200, false))))
        .isFalse();
  }

  @Test
  void aBorderUnderFiftyKmIsNotEligible() {
    CountryShape shortA = shape("A", 1000, neighbour("B", 49, true));
    CountryShape exactlyA = shape("A", 1000, neighbour("B", 50, true));

    assertThat(CountryPairEligibility.borderShapes().test(shortA, b)).isFalse();
    assertThat(CountryPairEligibility.borderShapes().test(exactlyA, b)).isTrue();
  }

  @Test
  void mergedCountriesNeedSimilarSizesButBorderShapesDoNot() {
    CountryShape huge = shape("B", 8001, neighbour("A", 200, true));
    CountryShape eightTimes = shape("B", 8000, neighbour("A", 200, true));

    assertThat(CountryPairEligibility.mergedCountries().test(a, huge)).isFalse();
    assertThat(CountryPairEligibility.borderShapes().test(a, huge)).isTrue();
    assertThat(CountryPairEligibility.mergedCountries().test(a, eightTimes)).isTrue();
    // The larger one may come first by name.
    assertThat(
            CountryPairEligibility.mergedCountries()
                .test(
                    shape("A", 8001, neighbour("B", 200, true)),
                    shape("B", 1000, neighbour("A", 200, true))))
        .isFalse();
  }

  @Test
  void aCountryWithNoAreaIsNotMerged() {
    CountryShape noArea = shape("B", 0, neighbour("A", 200, true));

    assertThat(CountryPairEligibility.mergedCountries().test(a, noArea)).isFalse();
  }

  @Test
  void neighbourFindsTheEntryOrNull() {
    assertThat(CountryPairEligibility.neighbour(a, b))
        .isNotNull()
        .extracting(Neighbour::km)
        .isEqualTo(200.0);
    assertThat(CountryPairEligibility.neighbour(a, shape("Z", 1))).isNull();
  }
}
