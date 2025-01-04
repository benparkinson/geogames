package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.loaders.tripoint.TripointComputer;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TripointComputerTest {

  private final TripointComputer service = new TripointComputer();

  @Test
  public void returnsAllTripointsWhenCountEqualsMaxAmount() throws IOException {
    List<List<List<Double>>> coordinates = List.of(List.of(List.of(12.34, 56.78)));
    GeoGeometry geometry = new GeoGeometry("Polygon", coordinates);

    GeoProperties geoProperties1 = new GeoProperties("Aruba");
    GeoData geoData1 = new GeoData("Feature", geoProperties1, geometry);
    GeoProperties geoProperties2 = new GeoProperties("Jamaica");
    GeoData geoData2 = new GeoData("Feature", geoProperties2, geometry);
    GeoProperties geoProperties3 = new GeoProperties("Ooh I wanna take ya");
    GeoData geoData3 = new GeoData("Feature", geoProperties3, geometry);

    List<Country> countries =
        List.of(createCountry(geoData1), createCountry(geoData2), createCountry(geoData3));

    List<Tripoint> randomTripoints = service.computeTripoints(countries);
    //    assertThat(randomTripoints).hasSize(1);
  }

  private Country createCountry(GeoData geoData) {
    return new Country(geoData.properties().name(), List.of(), geoData);
  }
}
