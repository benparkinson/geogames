package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.Bounds;
import com.parkinson.benjamin.geogames.model.CountryRivers;
import com.parkinson.benjamin.geogames.model.CountryRiversIndexEntry;
import com.parkinson.benjamin.geogames.model.CountryRiversRound;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class CountryRiversRoundGeneratorTest {

  private final Random random = new Random(3);

  private final CountryRiversService rivers = mock(CountryRiversService.class);
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

  private static CountryRiversIndexEntry entry(String name, int riverCount, long riverKm) {
    return new CountryRiversIndexEntry(name, name + ".json", riverCount, riverKm, 0);
  }

  private final CountryRiversIndexEntry a = entry("A", 5, 1_000);
  private final CountryRiversIndexEntry b = entry("B", 20, 9_000);
  private final CountryRiversIndexEntry fewRivers = entry("Few", 4, 9_000);
  private final CountryRiversIndexEntry short_ = entry("Short", 20, 999);
  private final CountryRiversIndexEntry noShape = entry("NoShape", 20, 9_000);

  @BeforeEach
  void setUp() {
    List<CountryRiversIndexEntry> index = List.of(a, b, fewRivers, short_, noShape);
    when(rivers.index()).thenReturn(index);
    for (CountryRiversIndexEntry e : index) {
      when(rivers.find(e.name())).thenReturn(e);
      when(rivers.load(e.name()))
          .thenAnswer(
              call ->
                  new CountryRivers(
                      e.name(),
                      data(e.name(), "MultiLineString"),
                      data(e.name(), "MultiLineString"),
                      data(e.name(), "MultiPolygon")));
    }
    List<CountryShape> all = List.of(shape("A"), shape("B"), shape("Few"), shape("Short"));
    when(shapes.loadShapes()).thenReturn(all);
    for (CountryShape s : all) {
      when(shapes.loadShape(s.name())).thenReturn(s);
      when(shapes.findShape(s.name())).thenReturn(Optional.of(s));
    }
  }

  private CountryRiversRoundGenerator generator() {
    return new CountryRiversRoundGenerator(rivers, shapes);
  }

  @Test
  void generatesRoundsForCountriesWithEnoughRiversAndAShape() {
    CountryRiversRoundGenerator generator = generator();

    assertThat(generator.gameType()).isSameAs(GameType.COUNTRIES_BY_RIVERS);
    List<GameData> rounds = generator.generate(2, Set.of(), random);
    assertThat(rounds)
        .extracting(r -> ((CountryRiversRound) r).country().name())
        .containsExactlyInAnyOrder("A", "B");
    assertThat(rounds)
        .allSatisfy(r -> assertThat(r.gameType()).isSameAs(GameType.COUNTRIES_BY_RIVERS));
    assertThatThrownBy(() -> generator.generate(3, Set.of(), random))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("countries");
  }

  @Test
  void readsNoGeometryUntilACountryIsPicked() {
    generator().generate(1, Set.of("country:A"), random);

    verify(rivers).load("B");
    verify(rivers, never()).load("A");
    verify(rivers, never()).load("Few");
    verify(rivers, never()).load("Short");
    verify(rivers, never()).load("NoShape");
  }

  @Test
  void skipsExcludedSubjects() {
    List<GameData> rounds = generator().generate(1, Set.of("country:A"), random);

    assertThat(rounds).extracting(GameData::subjectId).containsExactly("country:B");
  }

  @Test
  void carriesTheCountrysShapeAndItsFilesGeometry() {
    CountryRiversRound round =
        (CountryRiversRound) generator().generate(1, Set.of("country:A"), random).get(0);

    assertThat(round.subjectId()).isEqualTo("country:B");
    assertThat(round.roundKey()).isEqualTo("country:B");
    assertThat(round.country()).isSameAs(shapes.loadShape("B"));
    assertThat(round.major().properties().name()).isEqualTo("B");
    assertThat(round.minor().geometry().type()).isEqualTo("MultiLineString");
    assertThat(round.lakes().geometry().type()).isEqualTo("MultiPolygon");
  }

  @Test
  void rebuildGivesBackEveryGeneratedRoundAsIdenticalJson() {
    ObjectMapper mapper = JsonMapper.builder().build();
    CountryRiversRoundGenerator generator = generator();

    for (GameData round : generator.generate(2, Set.of(), random)) {
      GameData rebuilt = generator.rebuild(round.roundKey());

      assertThat(rebuilt).isEqualTo(round);
      assertThat(mapper.writeValueAsString(rebuilt)).isEqualTo(mapper.writeValueAsString(round));
    }
  }

  @Test
  void rebuildIgnoresTheEligibilityThresholds() {
    assertThat(generator().rebuild("country:Few").subjectId()).isEqualTo("country:Few");
    assertThat(generator().rebuild("country:Short").subjectId()).isEqualTo("country:Short");
  }

  @Test
  void rebuildRejectsUnknownCountriesCountriesWithoutAShapeAndWrongPrefixes() {
    CountryRiversRoundGenerator generator = generator();

    for (String key : List.of("country:Nowhere", "country:NoShape", "river:A", "pair:A|B")) {
      assertThatThrownBy(() -> generator.rebuild(key))
          .as(key)
          .isInstanceOf(RoundDataMissingException.class);
    }
    verify(rivers, never()).load(anyString());
  }
}
