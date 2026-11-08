package com.parkinson.benjamin.geogames.loaders.countries;

import com.parkinson.benjamin.geogames.model.Country;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Usage: CountryLoaderMain &lt;countries.geo.json&gt; &lt;additionalNames.json&gt;
 * &lt;excludedNames.json&gt; &lt;outDir&gt;
 *
 * <p>Clears the {@code *.json} files in outDir before writing. The directory must already exist.
 */
public class CountryLoaderMain {

  /**
   * What a run produced. The program prints nothing.
   *
   * @param countries the countries written
   */
  public record Report(List<Country> countries) {}

  public static void main(String[] args) throws IOException {
    run(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
  }

  /** Does everything {@code main} does. */
  static Report run(
      Path countriesFile, Path additionalNamesFile, Path excludedNamesFile, Path outDir)
      throws IOException {
    List<Country> countries =
        new RawCountryReader().readCountries(countriesFile, additionalNamesFile, excludedNamesFile);

    CountryWriter countryWriter = new CountryWriter();
    countryWriter.clearOutput(outDir);
    countryWriter.writeCountries(countries, outDir);
    return new Report(countries);
  }
}
