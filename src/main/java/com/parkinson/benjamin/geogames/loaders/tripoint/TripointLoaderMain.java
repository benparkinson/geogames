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

  public static void main(String[] args) throws IOException {
    List<Country> countries = new ProcessedCountryReader().readCountries(Path.of(args[0]));

    TripointComputer tripointComputer = new TripointComputer();
    List<Tripoint> tripoints = tripointComputer.computeTripoints(countries);

    TripointSnapper snapper = new TripointSnapper();
    List<TripointDefinition> definitions = new ArrayList<>();
    for (Tripoint tripoint : tripoints) {
      List<Country> tripointCountries = new ArrayList<>(tripoint.countries());
      List<String> names = tripointCountries.stream().map(Country::name).toList();
      Optional<com.parkinson.benjamin.geogames.model.Coordinate> snapped =
          snapper.snap(tripoint.coordinate(), tripointCountries);
      if (snapped.isPresent()) {
        definitions.add(new TripointDefinition(snapped.get(), names));
      } else {
        System.out.println(
            "Dropping " + names + ": no border junction near " + tripoint.coordinate());
      }
    }

    new TripointWriter().writeTripoints(definitions, Path.of(args[1]));
  }
}
