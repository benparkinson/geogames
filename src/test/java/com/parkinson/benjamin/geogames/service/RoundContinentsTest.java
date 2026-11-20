package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.CountryCitiesRound;
import com.parkinson.benjamin.geogames.model.CountryPairRound;
import com.parkinson.benjamin.geogames.model.CountryRiversRound;
import com.parkinson.benjamin.geogames.model.CountryShapeRound;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.Lake;
import com.parkinson.benjamin.geogames.model.LakeRound;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.RiverRound;
import com.parkinson.benjamin.geogames.model.Tripoint;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** Against the real country shapes, whose continents come from {@code continents.json}. */
class RoundContinentsTest {

  private final CountryShapeService shapes =
      new CountryShapeService(new JsonResourceReader(new PathMatchingResourcePatternResolver()));
  private final RoundContinents continents = new RoundContinents(shapes);

  private CountryShapeRound countryRound(String name) {
    return new CountryShapeRound(GameType.COUNTRIES_BY_COASTLINE, shapes.loadShape(name), null);
  }

  @Test
  void aCountryRoundShowsItsContinent() {
    assertThat(continents.of(countryRound("France"))).containsExactly("Europe");
    assertThat(continents.of(countryRound("Brazil"))).containsExactly("South America");
    assertThat(continents.of(countryRound("Australia"))).containsExactly("Oceania");
  }

  @Test
  void aCountryOnTwoContinentsShowsBothInContinentOrder() {
    assertThat(continents.of(countryRound("Russia"))).containsExactly("Asia", "Europe");
    assertThat(continents.of(countryRound("Türkiye"))).containsExactly("Asia", "Europe");
    assertThat(continents.of(countryRound("Egypt"))).containsExactly("Africa", "Asia");
  }

  @Test
  void theCitiesAndCountryRiversRoundsUseTheirCountry() {
    assertThat(
            continents.of(
                new CountryCitiesRound(
                    GameType.COUNTRIES_BY_CITIES, shapes.loadShape("Japan"), List.of())))
        .containsExactly("Asia");
    assertThat(
            continents.of(
                new CountryRiversRound(
                    GameType.COUNTRIES_BY_RIVERS, shapes.loadShape("Peru"), null, null, null)))
        .containsExactly("South America");
  }

  @Test
  void aPairAcrossTwoContinentsShowsTheUnion() {
    CountryPairRound pair =
        new CountryPairRound(
            GameType.BORDERS_BY_SHAPE,
            List.of(shapes.loadShape("Poland"), shapes.loadShape("Kazakhstan")),
            null,
            0,
            null,
            null,
            null);

    assertThat(continents.of(pair)).containsExactly("Asia", "Europe");
  }

  @Test
  void aPairOnOneContinentShowsItOnce() {
    CountryPairRound pair =
        new CountryPairRound(
            GameType.MERGED_COUNTRIES,
            List.of(shapes.loadShape("France"), shapes.loadShape("Spain")),
            null,
            0,
            null,
            null,
            null);

    assertThat(continents.of(pair)).containsExactly("Europe");
  }

  @Test
  void aTripointShowsEveryContinentOfItsCountries() {
    Tripoint tripoint =
        new Tripoint(
            new Coordinate(0, 0),
            Set.of(
                new Country("Russia", List.of(), null),
                new Country("China", List.of(), null),
                new Country("Mongolia", List.of(), null)));

    assertThat(continents.of(tripoint)).containsExactly("Asia", "Europe");
  }

  @Test
  void aRiverOrLakeShowsItsOwnContinent() {
    River river = new River("Q1", "Nile", List.of(), "Africa", null, null, null, List.of());
    Lake lake = new Lake("Q2", "Taupo", List.of(), "Oceania", null, List.of());

    assertThat(continents.of(new RiverRound(GameType.RIVERS_BY_SHAPE, river, null)))
        .containsExactly("Africa");
    assertThat(continents.of(new LakeRound(GameType.LAKES_BY_SHAPE, lake)))
        .containsExactly("Oceania");
  }

  @Test
  void aRiverWithTheOldContinentNameIsAnError() {
    River river = new River("Q1", "Murray", List.of(), "Australia", null, null, null, List.of());

    assertThatThrownBy(() -> continents.of(new RiverRound(GameType.RIVERS_BY_SHAPE, river, null)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void anUnknownKindOfRoundIsAnError() {
    GameData unknown =
        new GameData() {
          @Override
          public GameType gameType() {
            return GameType.TRIPOINT;
          }

          @Override
          public String subjectId() {
            return "x";
          }
        };

    assertThatThrownBy(() -> continents.of(unknown)).isInstanceOf(IllegalStateException.class);
  }
}
