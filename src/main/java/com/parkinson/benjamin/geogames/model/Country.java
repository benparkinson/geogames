package com.parkinson.benjamin.geogames.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(exclude = "coordinates")
public class Country {

  private String name;
  private Coordinate[] coordinates;

}
