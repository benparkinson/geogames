package com.parkinson.benjamin.geogames.loaders.cities;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.loaders.CapturedOutput;
import com.parkinson.benjamin.geogames.loaders.cities.CityLoaderMain.CapitalKind;
import com.parkinson.benjamin.geogames.loaders.cities.CityLoaderMain.Place;
import com.parkinson.benjamin.geogames.model.City;
import com.parkinson.benjamin.geogames.model.CountryCities;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

class CityLoaderMainTest {

  @TempDir Path out;

  private Path places;
  private Path countries;

  @BeforeEach
  void locateFixtures() throws URISyntaxException {
    places = Path.of(getClass().getResource("/fixtures/loaders/cities/places.geojson").toURI());
    countries = Path.of(getClass().getResource("/fixtures/loaders/countries-processed").toURI());
  }

  private static City city(String name, int population) {
    return new City(name, 0, 0, population, false);
  }

  private static CountryCities find(CityLoaderMain.Report report, String country) {
    return report.countries().stream()
        .filter(c -> c.country().equals(country))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void assignsEachCityToItsCountryLargestFirstWithRoundedCoordinates() throws IOException {
    CityLoaderMain.Report report =
        CityLoaderMain.run(places, countries, out.resolve("cities.json"));

    assertThat(report.countries())
        .extracting(CountryCities::country)
        .containsExactly("Alpha", "Beta");
    CountryCities alpha = find(report, "Alpha");
    assertThat(alpha.cities())
        .extracting(City::name)
        .containsExactly("Big", "Coast", "Capitalville");
    assertThat(alpha.cities().get(0).lon()).isEqualTo(5.1235);
    assertThat(alpha.cities().get(0).lat()).isEqualTo(5.6543);
    assertThat(alpha.cities().get(0).population()).isEqualTo(1000);
  }

  @Test
  void aCityJustOutsideItsPolygonIsFoundByLookingAround() throws IOException {
    CityLoaderMain.Report report =
        CityLoaderMain.run(places, countries, out.resolve("cities.json"));

    assertThat(find(report, "Alpha").cities()).extracting(City::name).contains("Coast");
    assertThat(report.assignedByNudge()).isEqualTo(1);
  }

  @Test
  void aCityInNoPolygonGoesToTheCountryItsAdm0nameNamesOrIsDropped() throws IOException {
    CityLoaderMain.Report report =
        CityLoaderMain.run(places, countries, out.resolve("cities.json"));

    assertThat(find(report, "Beta").cities()).extracting(City::name).contains("Offshore");
    assertThat(report.assignedByName()).isEqualTo(1);
    assertThat(report.dropped()).containsExactly("Lost (Nowhere)");
  }

  @Test
  void flagsTheNationalCapitalEvenWhenAdm0capIsZeroAndNotARegionCapital() throws IOException {
    CityLoaderMain.Report report =
        CityLoaderMain.run(places, countries, out.resolve("cities.json"));

    assertThat(find(report, "Alpha").cities())
        .filteredOn(City::capital)
        .extracting(City::name)
        .containsExactly("Capitalville");
    assertThat(find(report, "Beta").cities())
        .filteredOn(City::capital)
        .extracting(City::name)
        .containsExactly("Betaville");
  }

  @Test
  void aMissingPopulationIsZeroAndAnEmptyPointIsSkipped() throws IOException {
    CityLoaderMain.Report report =
        CityLoaderMain.run(places, countries, out.resolve("cities.json"));

    assertThat(find(report, "Beta").cities())
        .filteredOn(c -> c.name().equals("No Population"))
        .extracting(City::population)
        .containsExactly(0);
    assertThat(find(report, "Beta").cities()).extracting(City::name).doesNotContain("No Point");
  }

  @Test
  void writesOneFileTheServiceCanReadAndRunningTwiceGivesTheSameBytes() throws IOException {
    Path file = out.resolve("nested/cities.json");
    CityLoaderMain.run(places, countries, file);
    String first = Files.readString(file);
    CityLoaderMain.run(places, countries, file);

    assertThat(Files.readString(file)).isEqualTo(first);
    JsonNode root = GeoJson.lenientReader().readTree(first);
    assertThat(root.isArray()).isTrue();
    assertThat(root.get(0).path("country").asString()).isEqualTo("Alpha");
    assertThat(root.get(0).path("cities").get(0).path("name").asString()).isEqualTo("Big");
  }

  @Test
  void keepsTheLargestFortyAndAlwaysEveryCapital() {
    List<City> cities = new ArrayList<>();
    for (int i = 0; i < 60; i++) {
      cities.add(city("City " + String.format("%02d", i), 1000 - i));
    }
    cities.add(new City("Small capital", 0, 0, 1, true));

    List<City> kept = CityLoaderMain.keep(cities);

    assertThat(kept).hasSize(41);
    assertThat(kept.get(0).name()).isEqualTo("City 00");
    assertThat(kept.get(39).name()).isEqualTo("City 39");
    assertThat(kept.get(40).name()).isEqualTo("Small capital");
  }

  @Test
  void citiesOfTheSamePopulationAreOrderedByName() {
    List<City> kept = CityLoaderMain.keep(List.of(city("B", 5), city("A", 5), city("C", 9)));

    assertThat(kept).extracting(City::name).containsExactly("C", "A", "B");
  }

  @Test
  void aCountryWithOnlyRegionCapitalsFlagsTheLargestOneAlone() {
    List<Place> places =
        List.of(
            new Place(city("Small region capital", 10), CapitalKind.REGION),
            new Place(city("Nuuk", 500), CapitalKind.REGION),
            new Place(city("Plain", 900), CapitalKind.NONE));

    assertThat(CityLoaderMain.withCapital(places))
        .filteredOn(City::capital)
        .extracting(City::name)
        .containsExactly("Nuuk");
  }

  @Test
  void aNationalCapitalMeansNoRegionCapitalIsFlagged() {
    List<Place> places =
        List.of(
            new Place(city("Cardiff", 300), CapitalKind.REGION),
            new Place(city("London", 900), CapitalKind.NATIONAL));

    assertThat(CityLoaderMain.withCapital(places))
        .filteredOn(City::capital)
        .extracting(City::name)
        .containsExactly("London");
  }

  @Test
  void aCountryWithSeveralNationalCapitalsFlagsAllOfThem() {
    List<Place> places =
        List.of(
            new Place(city("La Paz", 900), CapitalKind.NATIONAL),
            new Place(city("Sucre", 300), CapitalKind.NATIONAL),
            new Place(city("Santa Cruz", 1500), CapitalKind.NONE));

    assertThat(CityLoaderMain.withCapital(places))
        .filteredOn(City::capital)
        .extracting(City::name)
        .containsExactlyInAnyOrder("La Paz", "Sucre");
  }

  @Test
  void theSummaryReportsTheCountsTheDroppedCitiesAndTheEligibleCountries() throws Exception {
    String output =
        CapturedOutput.of(
            () ->
                CityLoaderMain.main(
                    new String[] {
                      places.toString(), countries.toString(), out.resolve("cities.json").toString()
                    }));

    assertThat(output)
        .contains("Countries: 2, cities: 7")
        .contains("Dropped, in no country: 1")
        .contains("Lost (Nowhere)")
        .contains("Eligible (at least 8 cities): 0")
        .contains("UNDER 80")
        .contains("Without a capital: 0");
  }
}
