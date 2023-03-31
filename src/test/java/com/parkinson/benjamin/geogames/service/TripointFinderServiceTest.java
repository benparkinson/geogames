package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.geojson.CountryGeoData;
import com.parkinson.benjamin.geogames.model.geojson.CountryGeometry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TripointFinderServiceTest {

  private TripointFinderService service = new TripointFinderService();

  @Test
  public void findsSimpleCoordinates() {
    List<List<List<Double>>> coordinates = List.of(List.of(List.of(12.34, 56.78)));
    CountryGeometry geometry = new CountryGeometry("Polygon", coordinates);
    geometry.setCoordinates(coordinates);

    List<Coordinate> output = service.getCoordinates(geometry);

    assertThat(output).hasSize(1);
    assertThat(output).containsOnly(new Coordinate(56.78, 12.34));
  }

}
