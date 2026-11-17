package com.parkinson.benjamin.geogames.loaders.cities;

import com.parkinson.benjamin.geogames.helper.CountryCitiesEligibility;
import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.loaders.countries.ProcessedCountryReader;
import com.parkinson.benjamin.geogames.loaders.features.AliasBuilder;
import com.parkinson.benjamin.geogames.loaders.features.CountryLocator;
import com.parkinson.benjamin.geogames.model.City;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.CountryCities;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.databind.JsonNode;

/**
 * Usage: CityLoaderMain &lt;populatedPlaces.geojson&gt; &lt;countriesDir&gt; &lt;outFile&gt;
 *
 * <p>Assigns each Natural Earth populated place to the country whose polygon it is in, keeps each
 * country's {@link #MAX_CITIES} largest cities and always its capital, and writes the single file
 * {@code processed/cities.json}: an array of {@link CountryCities}, countries sorted by name and
 * cities by population (largest first) then name. Offline and deterministic.
 */
public class CityLoaderMain {

  /** How many cities each country keeps, besides its capital. */
  static final int MAX_CITIES = 40;

  /** A coastal city can fall just outside its polygon: look this far (about 3 km) around it. */
  static final double NUDGE_DEGREES = 0.03;

  private static final int DECIMALS = 10_000;

  /**
   * What a run produced, for the summary.
   *
   * @param countries what was written
   * @param dropped the names of the cities that could not be given a country
   * @param assignedByNudge how many cities only got a country by looking around them
   * @param assignedByName how many cities only got a country from their {@code adm0name}
   */
  public record Report(
      List<CountryCities> countries,
      List<String> dropped,
      int assignedByNudge,
      int assignedByName) {}

  public static void main(String[] args) throws IOException {
    Report report = run(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]));
    printSummary(report);
  }

  /** Does everything {@code main} does except print. */
  static Report run(Path populatedPlaces, Path countriesDir, Path outFile) throws IOException {
    List<Country> countries = new ProcessedCountryReader().readCountries(countriesDir);
    CountryLocator locator = new CountryLocator(countries, 0);
    Map<String, String> byName = namesByNormalisedName(countries);

    JsonNode collection;
    try (InputStream in = Files.newInputStream(populatedPlaces)) {
      collection = GeoJson.lenientReader().readTree(in);
    }

    Map<String, List<Place>> placesByCountry = new TreeMap<>();
    List<String> dropped = new ArrayList<>();
    int[] assigned = new int[2];
    for (JsonNode feature : collection.path("features")) {
      JsonNode props = feature.path("properties");
      JsonNode coordinates = feature.path("geometry").path("coordinates");
      if (coordinates.size() < 2) {
        continue;
      }
      double[] point = {coordinates.get(0).asDouble(), coordinates.get(1).asDouble()};
      String name = props.path("name").asString("");

      String country = locator.countryAt(point);
      if (country == null) {
        country = nearby(locator, point);
        if (country != null) {
          assigned[0]++;
        }
      }
      if (country == null) {
        country = byName.get(AliasBuilder.normalise(props.path("adm0name").asString("")));
        if (country != null) {
          assigned[1]++;
        }
      }
      if (country == null) {
        dropped.add(name + " (" + props.path("adm0name").asString("") + ")");
        continue;
      }
      placesByCountry
          .computeIfAbsent(country, key -> new ArrayList<>())
          .add(
              new Place(
                  new City(
                      name,
                      round(point[0]),
                      round(point[1]),
                      Math.max(0, props.path("pop_max").asInt(0)),
                      false),
                  capitalKind(props)));
    }

    List<CountryCities> result = new ArrayList<>();
    placesByCountry.forEach(
        (country, places) ->
            result.add(
                new CountryCities(
                    country, keep(applyCapitalOverride(country, withCapital(places))))));

    if (outFile.getParent() != null) {
      Files.createDirectories(outFile.getParent());
    }
    Files.writeString(outFile, GeoJson.prettyWriter().writeValueAsString(result) + "\n");
    dropped.sort(Comparator.naturalOrder());
    return new Report(result, dropped, assigned[0], assigned[1]);
  }

  /** What Natural Earth says about whether a place is a capital. */
  enum CapitalKind {
    NONE,
    /** The capital of a region of the country (Nuuk in Greenland; Cardiff in the United Kingdom). */
    REGION,
    NATIONAL
  }

  /** A city before its capital flag is settled. */
  record Place(City city, CapitalKind kind) {}

  /**
   * {@code adm0cap} is 1 for a capital, but Natural Earth leaves it at 0 for a few capitals that
   * its {@code featurecla} does name (Juba, the capital of South Sudan).
   */
  static CapitalKind capitalKind(JsonNode props) {
    String featureClass = props.path("featurecla").asString("");
    if (props.path("adm0cap").asInt(0) == 1 || featureClass.equals("Admin-0 capital")) {
      return CapitalKind.NATIONAL;
    }
    return featureClass.equals("Admin-0 region capital") ? CapitalKind.REGION : CapitalKind.NONE;
  }

  /**
   * Flags every national capital. A country with none (Greenland, which is not a country by
   * itself, has only the capital of its region) gets its largest regional capital instead. Other
   * regional capitals (Cardiff, Edinburgh) are not flagged, so a country has one highlighted city
   * wherever Natural Earth has one.
   */
  static List<City> withCapital(List<Place> places) {
    boolean national = places.stream().anyMatch(p -> p.kind() == CapitalKind.NATIONAL);
    CapitalKind wanted = national ? CapitalKind.NATIONAL : CapitalKind.REGION;
    List<City> cities = new ArrayList<>();
    boolean regionTaken = false;
    List<Place> bySize = new ArrayList<>(places);
    bySize.sort(Comparator.comparingInt((Place p) -> p.city().population()).reversed());
    for (Place place : bySize) {
      boolean capital = place.kind() == wanted && (national || !regionTaken);
      regionTaken |= capital;
      City c = place.city();
      cities.add(new City(c.name(), c.lon(), c.lat(), c.population(), capital));
    }
    return cities;
  }

  /**
   * Countries where Natural Earth flags the wrong cities, with the cities that really are capitals.
   * Benin's official capital is Porto-Novo (Cotonou is the seat of government in practice) and
   * Tanzania's is Dodoma (Dar es Salaam is the commercial centre). Myanmar moved its capital from Yangon to Naypyidaw in 2006; Ivory Coast's official capital is
   * Yamoussoukro (Abidjan is the economic centre); South Africa has three capitals (Pretoria
   * executive, Cape Town legislative, Bloemfontein judicial) and Johannesburg is none of them.
   * Kazakhstan's capital was renamed Astana in 2022.
   */
  static final Map<String, List<String>> CAPITAL_OVERRIDES =
      Map.of(
          "Benin", List.of("Porto-Novo"),
          "Myanmar", List.of("Naypyidaw"),
          "Ivory Coast", List.of("Yamoussoukro"),
          "South Africa", List.of("Pretoria", "Cape Town", "Bloemfontein"),
          "Tanzania", List.of("Dodoma"));

  /** Renames for cities Natural Earth still calls by an old name. */
  static final Map<String, Map<String, String>> CITY_RENAMES =
      Map.of("Kazakhstan", Map.of("Nur-Sultan", "Astana"));

  /** Applies {@link #CAPITAL_OVERRIDES} and {@link #CITY_RENAMES} to one country's cities. */
  static List<City> applyCapitalOverride(String country, List<City> cities) {
    List<String> capitals = CAPITAL_OVERRIDES.get(country);
    Map<String, String> renames = CITY_RENAMES.getOrDefault(country, Map.of());
    List<City> result = new ArrayList<>();
    for (City c : cities) {
      boolean capital = capitals == null ? c.capital() : capitals.contains(c.name());
      String name = renames.getOrDefault(c.name(), c.name());
      result.add(new City(name, c.lon(), c.lat(), c.population(), capital));
    }
    return result;
  }

  /** The country at one of the four points just north, south, east and west, in that order. */
  private static String nearby(CountryLocator locator, double[] point) {
    double[][] offsets = {
      {0, NUDGE_DEGREES}, {0, -NUDGE_DEGREES}, {NUDGE_DEGREES, 0}, {-NUDGE_DEGREES, 0}
    };
    for (double[] offset : offsets) {
      String country = locator.countryAt(new double[] {point[0] + offset[0], point[1] + offset[1]});
      if (country != null) {
        return country;
      }
    }
    return null;
  }

  private static Map<String, String> namesByNormalisedName(List<Country> allCountries) {
    Map<String, String> byName = new HashMap<>();
    // Antarctica has no shape, so the games can't use it; its research stations are dropped.
    List<Country> countries =
        allCountries.stream().filter(c -> !"Antarctica".equals(c.name())).toList();
    for (Country country : countries) {
      for (String alias :
          country.additionalNames() == null ? List.<String>of() : country.additionalNames()) {
        byName.putIfAbsent(AliasBuilder.normalise(alias), country.name());
      }
    }
    // The names themselves win over another country's alias.
    for (Country country : countries) {
      byName.put(AliasBuilder.normalise(country.name()), country.name());
    }
    return byName;
  }

  /** The {@link #MAX_CITIES} largest and every capital, largest first, ties by name. */
  static List<City> keep(List<City> cities) {
    List<City> sorted = new ArrayList<>(cities);
    sorted.sort(Comparator.comparingInt(City::population).reversed().thenComparing(City::name));
    List<City> kept = new ArrayList<>();
    for (int i = 0; i < sorted.size(); i++) {
      if (i < MAX_CITIES || sorted.get(i).capital()) {
        kept.add(sorted.get(i));
      }
    }
    return kept;
  }

  private static double round(double v) {
    return Math.round(v * DECIMALS) / (double) DECIMALS;
  }

  private static void printSummary(Report report) {
    List<CountryCities> countries = report.countries();
    int cities = countries.stream().mapToInt(c -> c.cities().size()).sum();
    System.out.println("Countries: " + countries.size() + ", cities: " + cities);
    System.out.println(
        "Given a country by looking around them: "
            + report.assignedByNudge()
            + ", by their adm0name: "
            + report.assignedByName());
    System.out.println("Dropped, in no country: " + report.dropped().size());
    report.dropped().stream().limit(20).forEach(name -> System.out.println("  " + name));

    long eligible = countries.stream().filter(CountryCitiesEligibility.countryCities()).count();
    System.out.println(
        "Eligible (at least "
            + CountryCitiesEligibility.MIN_CITIES
            + " cities): "
            + eligible
            + (eligible < 80 ? "  <-- UNDER 80: report before lowering the threshold" : ""));
    List<String> noCapital =
        countries.stream()
            .filter(c -> c.cities().stream().noneMatch(City::capital))
            .map(CountryCities::country)
            .toList();
    System.out.println("Without a capital: " + noCapital.size() + " " + noCapital);
  }
}
