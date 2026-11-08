package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.model.PlaceName;
import com.parkinson.benjamin.geogames.service.CountryLoaderService;
import com.parkinson.benjamin.geogames.service.RiverLoaderService;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Names (and aliases) of the places in each family of games, for autocomplete and spell checks. */
@RestController
@RequestMapping("/api")
public class PlaceNameController {

  private final CountryLoaderService countryLoaderService;
  private final RiverLoaderService riverLoaderService;

  public PlaceNameController(
      CountryLoaderService countryLoaderService, RiverLoaderService riverLoaderService) {
    this.countryLoaderService = countryLoaderService;
    this.riverLoaderService = riverLoaderService;
  }

  @GetMapping("/countries")
  public ResponseEntity<List<PlaceName>> countries() {
    return cached(countryLoaderService.loadCountryNames());
  }

  @GetMapping("/rivers")
  public ResponseEntity<List<PlaceName>> rivers() {
    return cached(riverLoaderService.loadRiverNames());
  }

  private static ResponseEntity<List<PlaceName>> cached(List<PlaceName> names) {
    return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofDays(1))).body(names);
  }
}
