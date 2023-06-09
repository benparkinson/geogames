package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.service.CountryLoaderService;
import com.parkinson.benjamin.geogames.service.TripointFinderService;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TripointController {

  private final CountryLoaderService countryLoaderService;

  private final TripointFinderService tripointFinderService;

  @Autowired
  public TripointController(CountryLoaderService countryLoaderService,
      TripointFinderService tripointFinderService) {
    this.countryLoaderService = countryLoaderService;
    this.tripointFinderService = tripointFinderService;
  }

  @GetMapping("/api/tripoint")
  @CrossOrigin(origins = "http://localhost:3000")
  public Tripoint findRandomTripoint() throws IOException {
    List<Country> countries = countryLoaderService.loadCountries();
    return tripointFinderService.findTripoint(countries);
  }

}
