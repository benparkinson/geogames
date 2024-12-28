package com.parkinson.benjamin.geogames.loaders.tripoint;

import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.service.CountryLoaderService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Main {

  public static void main(String[] args) throws IOException {
    CountryLoaderService countryLoaderService = new CountryLoaderService();
    List<Country> countries = countryLoaderService.loadCountries();

    TripointComputer tripointComputer = new TripointComputer();
    List<Tripoint> tripoints = tripointComputer.computeTripoints(countries);

    TripointWriter tripointWriter = new TripointWriter();
    tripointWriter.writeTripoints(tripoints, Path.of(args[0]));
  }
}
