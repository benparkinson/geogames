package com.parkinson.benjamin.geogames.loaders.countries;

import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.service.CountryLoaderService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class CountryLoaderMain {

  public static void main(String[] args) throws IOException {
    CountryLoaderService countryLoaderService = new CountryLoaderService();
    List<Country> countries = countryLoaderService.loadCountries();

    CountryWriter countryWriter = new CountryWriter();
    countryWriter.writeCountries(countries, Path.of(args[0]));
  }
}
