package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TripointFinderServiceTest {

  private final TripointFinderService service = new TripointFinderService();

  @Test
  public void picksTripoint() {
    List<List<List<Double>>> coordinates = List.of(List.of(List.of(12.34, 56.78)));
    GeoGeometry geometry = new GeoGeometry("Polygon", coordinates);

    GeoProperties geoProperties1 = new GeoProperties("Aruba");
    GeoData geoData1 = new GeoData("Feature", geoProperties1, geometry);
    GeoProperties geoProperties2 = new GeoProperties("Jamaica");
    GeoData geoData2 = new GeoData("Feature", geoProperties2, geometry);
    GeoProperties geoProperties3 = new GeoProperties("Ooh I wanna take ya");
    GeoData geoData3 = new GeoData("Feature", geoProperties3, geometry);

    Tripoint tripoint = service.findTripoint(
        List.of(createCountry(geoData1), createCountry(geoData2), createCountry(geoData3)));

    assertThat(tripoint.coordinate()).isEqualTo(new Coordinate(56.78, 12.34));
  }

  @Test
  public void returnsAllTripointsWhenCountEqualsMaxAmount() {
    List<List<List<Double>>> coordinates = List.of(List.of(List.of(12.34, 56.78)));
    GeoGeometry geometry = new GeoGeometry("Polygon", coordinates);

    GeoProperties geoProperties1 = new GeoProperties("Aruba");
    GeoData geoData1 = new GeoData("Feature", geoProperties1, geometry);
    GeoProperties geoProperties2 = new GeoProperties("Jamaica");
    GeoData geoData2 = new GeoData("Feature", geoProperties2, geometry);
    GeoProperties geoProperties3 = new GeoProperties("Ooh I wanna take ya");
    GeoData geoData3 = new GeoData("Feature", geoProperties3, geometry);

    List<Country> countries = List.of(createCountry(geoData1), createCountry(geoData2), createCountry(geoData3));

    List<Tripoint> randomTripoints = service.findRandomTripoints(countries, 1);
    assertThat(randomTripoints).hasSize(1);
  }

  @Test
  public void rejectsRequestForTooManyTripoints() {
    List<List<List<Double>>> coordinates = List.of(List.of(List.of(12.34, 56.78)));
    GeoGeometry geometry = new GeoGeometry("Polygon", coordinates);

    GeoProperties geoProperties1 = new GeoProperties("Aruba");
    GeoData geoData1 = new GeoData("Feature", geoProperties1, geometry);
    List<Country> countries = List.of(createCountry(geoData1));

    assertThrows(IllegalArgumentException.class, () -> service.findRandomTripoints(countries, 1));
  }

  private Country createCountry(GeoData geoData) {
    return new Country(geoData.properties().name(), List.of(), geoData);
  }

}
