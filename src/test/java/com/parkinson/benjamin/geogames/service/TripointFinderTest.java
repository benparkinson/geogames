package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.parkinson.benjamin.geogames.loaders.tripoint.TripointComputer;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TripointFinderTest {

  private final TripointFinderService service = new TripointFinderService(null);

  @Test
  public void rejectsRequestForTooManyTripoints() {
    assertThrows(IllegalArgumentException.class, () -> service.findRandomTripoints(
        0, 1));
  }

}
