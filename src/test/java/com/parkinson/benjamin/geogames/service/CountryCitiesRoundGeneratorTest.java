package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.Bounds;
import com.parkinson.benjamin.geogames.model.City;
import com.parkinson.benjamin.geogames.model.CountryCities;
import com.parkinson.benjamin.geogames.model.CountryCitiesRound;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class CountryCitiesRoundGeneratorTest {

  private final Random random = new Random(3);

  private final CityService cities = mock(CityService.class);
  private final CountryShapeService shapes = mock(CountryShapeService.class);

  private static GeoData data(String name, String type) {
    return new GeoData("Feature", new GeoProperties(name), new GeoGeometry(type, List.of()));
  }

  private static CountryShape shape(String name) {
    return new CountryShape(
        name,
        List.of(),
        100,
        100,
        1000,
        List.of(),
        new Bounds(0, 0, 1, 1),
        data(name, "Polygon"),
        data(name, "MultiLineString"),
        data(name, "MultiLineString"));
  }

  private static CountryCities countryWith(String name, int cityCount) {
    List<City> list = new ArrayList<>();
    for (int i = 0; i < cityCount; i++) {
      list.add(new City(name + i, i, i, 1000 - i, i == 0));
    }
    return new CountryCities(name, list);
  }

  private final CountryCities a = countryWith("A", 8);
  private final CountryCities b = countryWith("B", 9);
  private final CountryCities few = countryWith("Few", 7);
  private final CountryCities noShape = countryWith("NoShape", 20);

  @BeforeEach
  void setUp() {
    when(cities.loadAll()).thenReturn(List.of(a, b, few, noShape));
    for (CountryCities c : List.of(a, b, few, noShape)) {
      when(cities.find(c.country())).thenReturn(c);
    }
    List<CountryShape> all = List.of(shape("A"), shape("B"), shape("Few"));
    when(shapes.loadShapes()).thenReturn(all);
    for (CountryShape s : all) {
      when(shapes.loadShape(s.name())).thenReturn(s);
    }
  }

  private CountryCitiesRoundGenerator generator() {
    return new CountryCitiesRoundGenerator(cities, shapes);
  }

  @Test
  void generatesRoundsOfTheCitiesTypeForCountriesWithEnoughCitiesAndAShape() {
    CountryCitiesRoundGenerator generator = generator();

    assertThat(generator.gameType()).isSameAs(GameType.COUNTRIES_BY_CITIES);
    List<GameData> rounds = generator.generate(2, Set.of(), random);
    assertThat(rounds)
        .extracting(r -> ((CountryCitiesRound) r).country().name())
        .containsExactlyInAnyOrder("A", "B");
    assertThat(rounds)
        .allSatisfy(r -> assertThat(r.gameType()).isSameAs(GameType.COUNTRIES_BY_CITIES));
    assertThatThrownBy(() -> generator.generate(3, Set.of(), random))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("countries");
  }

  @Test
  void skipsExcludedSubjects() {
    List<GameData> rounds = generator().generate(1, Set.of("country:A"), random);

    assertThat(rounds).extracting(GameData::subjectId).containsExactly("country:B");
  }

  @Test
  void usesTheCountrySubjectIdSoAMixedGameNeverAsksAboutACountryTwice() {
    CountryCitiesRound round =
        (CountryCitiesRound) generator().generate(1, Set.of("country:B"), random).get(0);

    assertThat(round.subjectId()).isEqualTo("country:A");
    assertThat(round.roundKey()).isEqualTo("country:A");
    assertThat(round.country()).isSameAs(shapes.loadShape("A"));
    assertThat(round.cities()).isEqualTo(a.cities());
  }

  @Test
  void rebuildGivesBackEveryGeneratedRoundAsIdenticalJson() {
    ObjectMapper mapper = JsonMapper.builder().build();
    CountryCitiesRoundGenerator generator = generator();

    for (GameData round : generator.generate(2, Set.of(), random)) {
      GameData rebuilt = generator.rebuild(round.roundKey());

      assertThat(rebuilt).isEqualTo(round);
      assertThat(mapper.writeValueAsString(rebuilt)).isEqualTo(mapper.writeValueAsString(round));
    }
  }

  @Test
  void rebuildIgnoresTheEligibilityThreshold() {
    assertThat(generator().rebuild("country:Few").subjectId()).isEqualTo("country:Few");
  }

  @Test
  void rebuildRejectsUnknownCountriesCountriesWithoutAShapeAndWrongPrefixes() {
    CountryCitiesRoundGenerator generator = generator();

    for (String key : List.of("country:Nowhere", "country:NoShape", "river:A", "pair:A|B")) {
      assertThatThrownBy(() -> generator.rebuild(key))
          .as(key)
          .isInstanceOf(RoundDataMissingException.class);
    }
  }
}
