package com.parkinson.benjamin.geogames.loaders.tripoint;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.TripointDefinition;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/** Only the offline part: the Wikipedia fetch in {@code main} is not tested. */
class TripointLoaderMainTest {

  @TempDir Path directory;

  /** A 0.1 degree square with one corner at (lon, lat), extending by dLon / dLat. */
  private static Country square(String name, double lon, double lat, int dLon, int dLat) {
    List<List<List<Double>>> ring =
        List.of(
            List.of(
                List.of(lon, lat),
                List.of(lon + dLon * 0.1, lat),
                List.of(lon + dLon * 0.1, lat + dLat * 0.1),
                List.of(lon, lat + dLat * 0.1),
                List.of(lon, lat)));
    return new Country(
        name,
        List.of(),
        new GeoData("Feature", new GeoProperties(name), new GeoGeometry("Polygon", ring)));
  }

  private static Set<Country> meeting(double lon, double lat) {
    return new LinkedHashSet<>(
        List.of(
            square("A", lon, lat, 1, 1),
            square("B", lon, lat, -1, 1),
            square("C", lon, lat, 1, -1)));
  }

  private TripointDefinition[] readBack(Path file) throws IOException {
    return JsonMapper.builder().build().readValue(file.toFile(), TripointDefinition[].class);
  }

  @Test
  void snapsEachTripointToTheJunctionAndWritesIt() throws IOException {
    Path file = directory.resolve("tripoints.json");

    TripointLoaderMain.Report report =
        TripointLoaderMain.snapAndWrite(
            List.of(new Tripoint(new Coordinate(20.01, 10.01), meeting(10.0, 20.0))), file);

    assertThat(report.dropped()).isEmpty();
    assertThat(report.definitions())
        .singleElement()
        .satisfies(
            definition -> {
              assertThat(definition.coordinate().latitude()).isEqualTo(20.0);
              assertThat(definition.coordinate().longitude()).isEqualTo(10.0);
              assertThat(definition.countryNames()).containsExactlyInAnyOrder("A", "B", "C");
            });
    assertThat(readBack(file)).containsExactlyElementsOf(report.definitions());
  }

  @Test
  void aTripointWithNoJunctionNearItIsDroppedAndNotWritten() throws IOException {
    Path file = directory.resolve("tripoints.json");
    Tripoint good = new Tripoint(new Coordinate(20.0, 10.0), meeting(10.0, 20.0));
    Tripoint far = new Tripoint(new Coordinate(50.0, 50.0), meeting(10.0, 20.0));

    TripointLoaderMain.Report report = TripointLoaderMain.snapAndWrite(List.of(good, far), file);

    assertThat(report.dropped()).containsExactly(far);
    assertThat(report.definitions()).hasSize(1);
    assertThat(readBack(file)).hasSize(1);
  }
}
