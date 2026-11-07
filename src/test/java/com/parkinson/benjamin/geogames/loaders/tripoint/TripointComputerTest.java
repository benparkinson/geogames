package com.parkinson.benjamin.geogames.loaders.tripoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TripointComputerTest {

  private final TripointComputer computer = new TripointComputer();

  private static Country country(String name, String... aliases) {
    return new Country(name, List.of(aliases), null);
  }

  private static Document fixture() throws IOException {
    try (InputStream stream =
        TripointComputerTest.class.getResourceAsStream("/fixtures/tripoints-sample.html")) {
      return Jsoup.parse(stream, "UTF-8", "");
    }
  }

  private final List<Country> countries =
      List.of(
          country("Alpha"),
          country("Beta"),
          country("Gamma"),
          country("Delta", "Delta-Alias"),
          country("Epsilon"),
          country("Zeta"));

  @Test
  void returnsOnlyRowsWithExactlyThreeKnownCountries() throws IOException {
    List<Tripoint> tripoints = computer.computeTripoints(countries, fixture());

    assertThat(tripoints).hasSize(2);
    assertThat(tripoints.get(0).countries())
        .extracting(Country::name)
        .containsExactlyInAnyOrder("Alpha", "Beta", "Gamma");
  }

  @Test
  void looksUpCountriesByAliasIgnoringCase() throws IOException {
    List<Tripoint> tripoints = computer.computeTripoints(countries, fixture());

    assertThat(tripoints.get(1).countries())
        .extracting(Country::name)
        .containsExactlyInAnyOrder("Delta", "Epsilon", "Zeta");
  }

  @Test
  void convertsDmsCoordinatesToDecimalDegrees() throws IOException {
    List<Tripoint> tripoints = computer.computeTripoints(countries, fixture());

    Coordinate first = tripoints.get(0).coordinate();
    assertThat(first.latitude()).isCloseTo(47.0 + 35.0 / 60.0, within(1e-9));
    assertThat(first.longitude()).isCloseTo(9.0 + 33.0 / 60.0, within(1e-9));

    Coordinate second = tripoints.get(1).coordinate();
    assertThat(second.latitude()).isCloseTo(-10.0, within(1e-9));
    assertThat(second.longitude()).isCloseTo(-20.0, within(1e-9));
  }

  @ParameterizedTest
  @CsvSource({
    "47°35′N, 47.583333333",
    "9°33′E, 9.55",
    "10°S, -10.0",
    "0°30′W, -0.5",
    "12°34′56.7″N, 12.582416667"
  })
  void convertsDmsStrings(String dms, double expected) {
    assertThat(computer.convertToDecimalCoordinate(dms)).isCloseTo(expected, within(1e-6));
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "47.5", "", "47°35′X"})
  void rejectsInvalidDmsStrings(String dms) {
    assertThatThrownBy(() -> computer.convertToDecimalCoordinate(dms))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
