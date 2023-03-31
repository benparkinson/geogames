package com.parkinson.benjamin.geogames.model;

import java.util.Set;
import com.parkinson.benjamin.geogames.model.geojson.CountryGeoData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Tripoint {

  private Coordinate coordinate;
  private Set<CountryGeoData> countries;

  public Tripoint(Coordinate coordinate, Set<CountryGeoData> countries) {
    this.coordinate = coordinate;
    this.countries = countries;
  }
}
