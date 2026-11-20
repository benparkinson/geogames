package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Continent;
import com.parkinson.benjamin.geogames.model.CountryCitiesRound;
import com.parkinson.benjamin.geogames.model.CountryPairRound;
import com.parkinson.benjamin.geogames.model.CountryRiversRound;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.CountryShapeRound;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.LakeRound;
import com.parkinson.benjamin.geogames.model.RiverRound;
import com.parkinson.benjamin.geogames.model.Tripoint;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The continents an Easy game shows from the start of a round: those of the country, countries,
 * river or lake the round is about, without repeats and in {@link Continent} order.
 */
@Component
public class RoundContinents {

  private final CountryShapeService shapes;

  public RoundContinents(CountryShapeService shapes) {
    this.shapes = shapes;
  }

  /** The display names of the round's continents. */
  public List<String> of(GameData data) {
    Set<Continent> continents = EnumSet.noneOf(Continent.class);
    switch (data) {
      case CountryShapeRound round -> addAll(continents, round.country());
      case CountryCitiesRound round -> addAll(continents, round.country());
      case CountryRiversRound round -> addAll(continents, round.country());
      case CountryPairRound round -> round.countries().forEach(c -> addAll(continents, c));
      case Tripoint tripoint ->
          tripoint.countries().forEach(c -> addAll(continents, shapes.loadShape(c.name())));
      case RiverRound round -> continents.add(Continent.fromLabel(round.river().continent()));
      case LakeRound round -> continents.add(Continent.fromLabel(round.lake().continent()));
      // GameData isn't sealed (a test implements it), so a new round type is caught by the Easy
      // check in GameApiIT, which plays every game type, and not by the compiler.
      default ->
          throw new IllegalStateException("No continents for " + data.getClass().getSimpleName());
    }
    return continents.stream().map(Continent::label).toList();
  }

  private static void addAll(Collection<Continent> continents, CountryShape shape) {
    shape.continents().forEach(label -> continents.add(Continent.fromLabel(label)));
  }
}
