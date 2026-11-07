package com.parkinson.benjamin.geogames.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.dao.GameType;
import com.parkinson.benjamin.geogames.model.Tripoint.TripointBuilder;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class TripointTest {

  private static Country country(String name) {
    GeoData geoData =
        new GeoData("Feature", new GeoProperties(name), new GeoGeometry("Polygon", List.of()));
    return new Country(name, List.of(), geoData);
  }

  @Test
  void builderPutsLatitudeFirstInTheCoordinate() {
    Tripoint tripoint = new TripointBuilder().latitude(12.5).longitude(-34.5).build();

    assertThat(tripoint.coordinate()).isEqualTo(new Coordinate(12.5, -34.5));
  }

  @Test
  void builderCollectsCountriesAndIgnoresDuplicates() {
    Country a = country("A");
    Country b = country("B");

    Tripoint tripoint = new TripointBuilder().country(a).country(b).country(a).build();

    assertThat(tripoint.countries()).containsExactlyInAnyOrder(a, b);
  }

  @Test
  void gameTypes() {
    assertThat(new TripointBuilder().build().gameType()).isSameAs(GameType.TRIPOINT);
    assertThat(new River("Nile", List.of(), "Africa", null).gameType())
        .isSameAs(GameType.RIVERS_BY_SHAPE);
  }
}
