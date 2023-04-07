package com.parkinson.benjamin.geogames.model;

import java.util.Set;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Tripoint {

  private Coordinate coordinate;
  private Set<GeoData> countries;

  public Tripoint(Coordinate coordinate, Set<GeoData> countries) {
    this.coordinate = coordinate;
    this.countries = countries;
  }
}
