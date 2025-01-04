package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.dao.GameData;
import com.parkinson.benjamin.geogames.dao.GameType;
import java.util.HashSet;
import java.util.Set;

public record Tripoint(Coordinate coordinate, Set<Country> countries) implements GameData {

  @Override
  public GameType gameType() {
    return GameType.TRIPOINT;
  }

  public static class TripointBuilder {

    private Set<Country> countries = new HashSet<>();
    private double latitude;
    private double longitude;

    public TripointBuilder() {}

    public TripointBuilder country(Country country) {
      countries.add(country);
      return this;
    }

    public TripointBuilder latitude(double latitude) {
      this.latitude = latitude;
      return this;
    }

    public TripointBuilder longitude(double longitude) {
      this.longitude = longitude;
      return this;
    }

    public Tripoint build() {
      return new Tripoint(new Coordinate(latitude, longitude), countries);
    }
  }
}
