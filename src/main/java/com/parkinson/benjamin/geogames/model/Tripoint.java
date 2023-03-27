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
  private Set<Country> countries;

  public Tripoint(Coordinate coordinate, Set<Country> countries) {
    this.coordinate = coordinate;
    this.countries = countries;
  }
}
