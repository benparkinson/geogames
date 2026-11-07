package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.model.CountryName;
import com.parkinson.benjamin.geogames.service.CountryLoaderService;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CountryController {

  private final CountryLoaderService countryLoaderService;

  public CountryController(CountryLoaderService countryLoaderService) {
    this.countryLoaderService = countryLoaderService;
  }

  @GetMapping("/countries")
  public ResponseEntity<List<CountryName>> countries() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
        .body(countryLoaderService.loadCountryNames());
  }
}
