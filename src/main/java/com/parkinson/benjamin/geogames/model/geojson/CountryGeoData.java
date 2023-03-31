package com.parkinson.benjamin.geogames.model.geojson;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CountryGeoData {

  private String type;
  private CountryProperties properties;
  private CountryGeometry geometry;

}
