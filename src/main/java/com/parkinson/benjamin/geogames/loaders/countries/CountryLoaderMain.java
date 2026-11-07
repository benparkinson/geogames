package com.parkinson.benjamin.geogames.loaders.countries;

import com.parkinson.benjamin.geogames.model.Country;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Usage: CountryLoaderMain &lt;countries.geo.json&gt; &lt;additionalNames.json&gt;
 * &lt;excludedNames.json&gt; &lt;outDir&gt;
 *
 * <p>Clears the {@code *.json} files in outDir before writing.
 */
public class CountryLoaderMain {

  public static void main(String[] args) throws IOException {
    List<Country> countries =
        new RawCountryReader().readCountries(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]));

    CountryWriter countryWriter = new CountryWriter();
    Path outDir = Path.of(args[3]);
    countryWriter.clearOutput(outDir);
    countryWriter.writeCountries(countries, outDir);
  }
}
