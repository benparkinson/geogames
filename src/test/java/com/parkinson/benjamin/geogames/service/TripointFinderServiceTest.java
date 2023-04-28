package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
        List.of(createCountry(geoData1), createCountry(geoData2), createCountry(geoData3)),
        Optional.empty());

    assertThat(tripoint.coordinate()).isEqualTo(new Coordinate(56.78, 12.34));
  }

  @Test
  public void doesNotPickLastTripoint() {
    List<List<List<Double>>> coordinatesToExclude = List.of(List.of(List.of(12.34, 56.78)));
    GeoGeometry geometry = new GeoGeometry("Polygon", coordinatesToExclude);
    List<List<List<Double>>> coordinates = List.of(List.of(List.of(23.45, 67.89)));
    GeoGeometry geometry2 = new GeoGeometry("Polygon", coordinates);

    GeoProperties geoProperties1 = new GeoProperties("Aruba");
    GeoData geoData1 = new GeoData("Feature", geoProperties1, geometry);
    GeoProperties geoProperties2 = new GeoProperties("Jamaica");
    GeoData geoData2 = new GeoData("Feature", geoProperties2, geometry);
    GeoProperties geoProperties3 = new GeoProperties("Ooh I wanna take ya");
    GeoData geoData3 = new GeoData("Feature", geoProperties3, geometry);
    GeoProperties geoProperties4 = new GeoProperties("Aruba1");
    GeoData geoData4 = new GeoData("Feature", geoProperties4, geometry2);
    GeoProperties geoProperties5 = new GeoProperties("Jamaica1");
    GeoData geoData5 = new GeoData("Feature", geoProperties5, geometry2);
    GeoProperties geoProperties6 = new GeoProperties("Ooh I wanna take ya1");
    GeoData geoData6 = new GeoData("Feature", geoProperties6, geometry2);

    Tripoint tripoint = service.findTripoint(
        List.of(createCountry(geoData1), createCountry(geoData2), createCountry(geoData3),
            createCountry(geoData4), createCountry(geoData5), createCountry(geoData6)),
        Optional.of(56.78));

    assertThat(tripoint.coordinate()).isEqualTo(new Coordinate(67.89, 23.45));
  }

  private Country createCountry(GeoData geoData) {
    return new Country(geoData.properties().name(), List.of(), geoData);
  }

}
