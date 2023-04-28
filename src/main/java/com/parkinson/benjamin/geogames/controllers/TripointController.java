package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.service.CountryLoaderService;
import com.parkinson.benjamin.geogames.service.TripointFinderService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TripointController {

  @Autowired
  private CountryLoaderService countryLoaderService;

  @Autowired
  private TripointFinderService tripointFinderService;

  @GetMapping("/api/tripoint")
  @CrossOrigin(origins = "http://localhost:3000")
  public Tripoint findRandomTripoint(@RequestParam(required = false) Optional<Double> lastLat) throws IOException {
    List<Country> countries = countryLoaderService.loadCountries();
    Tripoint tripoint = tripointFinderService.findTripoint(countries, lastLat);
    return tripoint;
  }

}
