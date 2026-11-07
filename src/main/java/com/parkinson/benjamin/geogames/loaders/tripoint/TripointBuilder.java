package com.parkinson.benjamin.geogames.loaders.tripoint;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import java.util.HashSet;
import java.util.Set;

/** Collects the pieces of a tripoint while the scraper walks a Wikipedia table row. */
class TripointBuilder {

  private final Set<Country> countries = new HashSet<>();
  private double latitude;
  private double longitude;

  TripointBuilder country(Country country) {
    countries.add(country);
    return this;
  }

  TripointBuilder latitude(double latitude) {
    this.latitude = latitude;
    return this;
  }

  TripointBuilder longitude(double longitude) {
    this.longitude = longitude;
    return this;
  }

  Tripoint build() {
    return new Tripoint(new Coordinate(latitude, longitude), countries);
  }
}
