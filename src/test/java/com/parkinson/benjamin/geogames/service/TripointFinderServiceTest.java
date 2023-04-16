package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TripointFinderServiceTest {

  private final TripointFinderService service = new TripointFinderService();

  @Test
  public void picksTripoint() {
    List<List<List<Double>>> coordinates = List.of(List.of(List.of(12.34, 56.78)));
    GeoGeometry geometry = new GeoGeometry("Polygon", coordinates);

    GeoData country1 = new GeoData();
    GeoProperties geoProperties1 = new GeoProperties();
    geoProperties1.setName("Aruba");
    country1.setProperties(geoProperties1);
    country1.setGeometry(geometry);
    GeoData country2 = new GeoData();
    GeoProperties geoProperties2 = new GeoProperties();
    geoProperties2.setName("Jamaica");
    country2.setProperties(geoProperties2);
    country2.setGeometry(geometry);
    GeoData country3 = new GeoData();
    GeoProperties geoProperties3 = new GeoProperties();
    geoProperties3.setName("Ooh I wanna take ya");
    country3.setProperties(geoProperties3);
    country3.setGeometry(geometry);

    Tripoint tripoint = service.findTripoint(List.of(country1, country2, country3));

    assertThat(tripoint.getCoordinate()).isEqualTo(new Coordinate(56.78, 12.34));
  }

}
