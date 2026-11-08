package com.parkinson.benjamin.geogames.loaders.tripoint;

import com.parkinson.benjamin.geogames.loaders.countries.ProcessedCountryReader;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Scrapes tripoints from Wikipedia, snaps them to the country geometry and writes one file.
 *
 * <p>Usage: TripointLoaderMain &lt;processedCountriesDir&gt; &lt;outFile&gt;
 */
public class TripointLoaderMain {

  /**
   * What the offline part produced.
   *
   * @param definitions the tripoints written, snapped to a border junction
   * @param dropped the tripoints with no border junction near them
   */
  public record Report(List<TripointDefinition> definitions, List<Tripoint> dropped) {}

  public static void main(String[] args) throws IOException {
    List<Country> countries = new ProcessedCountryReader().readCountries(Path.of(args[0]));

    // The only network step: scrapes Wikipedia's list of tripoints.
    TripointComputer tripointComputer = new TripointComputer();
    List<Tripoint> tripoints = tripointComputer.computeTripoints(countries);

    Report report = snapAndWrite(tripoints, Path.of(args[1]));
    for (Tripoint tripoint : report.dropped()) {
      System.out.println(
          "Dropping "
              + tripoint.countries().stream().map(Country::name).toList()
              + ": no border junction near "
              + tripoint.coordinate());
    }
  }

  /** The offline part: snaps each tripoint to the country geometry and writes the file. */
  static Report snapAndWrite(List<Tripoint> tripoints, Path outFile) throws IOException {
    TripointSnapper snapper = new TripointSnapper();
    List<TripointDefinition> definitions = new ArrayList<>();
    List<Tripoint> dropped = new ArrayList<>();
    for (Tripoint tripoint : tripoints) {
      List<Country> tripointCountries = new ArrayList<>(tripoint.countries());
      List<String> names = tripointCountries.stream().map(Country::name).toList();
      Optional<com.parkinson.benjamin.geogames.model.Coordinate> snapped =
          snapper.snap(tripoint.coordinate(), tripointCountries);
      if (snapped.isPresent()) {
        definitions.add(new TripointDefinition(snapped.get(), names));
      } else {
        dropped.add(tripoint);
      }
    }

    new TripointWriter().writeTripoints(definitions, outFile);
    return new Report(definitions, dropped);
  }
}
