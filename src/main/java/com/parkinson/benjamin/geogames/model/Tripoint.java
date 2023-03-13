package com.parkinson.benjamin.geogames.model;

import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Tripoint {

  private Coordinate coordinate;
  private Set<String> countryNames;

  public Tripoint(Coordinate coordinate, Set<String> countryNames) {
    this.coordinate = coordinate;
    this.countryNames = countryNames;
  }
}
