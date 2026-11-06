package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.model.CountryName;
import com.parkinson.benjamin.geogames.service.CountryLoaderService;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "https://geogauntlet.onrender.com"})
public class CountryController {

  private final CountryLoaderService countryLoaderService;

  @Autowired
  public CountryController(CountryLoaderService countryLoaderService) {
    this.countryLoaderService = countryLoaderService;
  }

  @GetMapping("api/countries")
  public ResponseEntity<List<CountryName>> countries() throws IOException {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
        .body(countryLoaderService.loadCountryNames());
  }
}
