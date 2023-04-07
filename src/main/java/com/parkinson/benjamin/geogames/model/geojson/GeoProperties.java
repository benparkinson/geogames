package com.parkinson.benjamin.geogames.model.geojson;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GeoProperties {

  private String name;

  private List<String> additionalNames;
}
