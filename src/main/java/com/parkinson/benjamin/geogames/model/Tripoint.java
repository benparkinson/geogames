package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.dao.GameData;
import java.util.Set;

public record Tripoint(Coordinate coordinate, Set<Country> countries) implements GameData {

}
