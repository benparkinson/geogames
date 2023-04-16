package com.parkinson.benjamin.geogames.model.geojson;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class GeoData {

  private String type;
  private GeoProperties properties;
  private GeoGeometry geometry;

}
