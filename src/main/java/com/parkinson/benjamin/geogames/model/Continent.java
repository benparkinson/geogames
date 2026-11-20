package com.parkinson.benjamin.geogames.model;

/** The six continents the game uses, in the order they are shown. */
public enum Continent {
  AFRICA("Africa"),
  ASIA("Asia"),
  EUROPE("Europe"),
  NORTH_AMERICA("North America"),
  SOUTH_AMERICA("South America"),
  OCEANIA("Oceania");

  private final String label;

  Continent(String label) {
    this.label = label;
  }

  /** The name the data files and the client use. */
  public String label() {
    return label;
  }

  /** The continent with this display name. Anything else, including "Australia", is an error. */
  public static Continent fromLabel(String label) {
    for (Continent continent : values()) {
      if (continent.label.equals(label)) {
        return continent;
      }
    }
    throw new IllegalArgumentException("Unknown continent: " + label);
  }
}
