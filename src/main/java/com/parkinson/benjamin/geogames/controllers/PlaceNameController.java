package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.model.LocatedPlaceName;
import com.parkinson.benjamin.geogames.service.CountryNameService;
import com.parkinson.benjamin.geogames.service.LakeLoaderService;
import com.parkinson.benjamin.geogames.service.RiverLoaderService;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Names (and aliases) of the places in each family of games, for autocomplete and spell checks. Responses are {@code no-cache}: the browser keeps its copy but revalidates it with the
 * ETag that {@code PlaceNameEtagConfig} adds, so a changed list shows at once and an unchanged one
 * costs a 304.
 */
@RestController
@RequestMapping("/api")
public class PlaceNameController {

  private final CountryNameService countryNameService;
  private final RiverLoaderService riverLoaderService;
  private final LakeLoaderService lakeLoaderService;

  public PlaceNameController(
      CountryNameService countryNameService,
      RiverLoaderService riverLoaderService,
      LakeLoaderService lakeLoaderService) {
    this.countryNameService = countryNameService;
    this.riverLoaderService = riverLoaderService;
    this.lakeLoaderService = lakeLoaderService;
  }

  @GetMapping("/countries")
  public ResponseEntity<List<LocatedPlaceName>> countries() {
    return cached(countryNameService.loadCountryNames());
  }

  @GetMapping("/rivers")
  public ResponseEntity<List<LocatedPlaceName>> rivers() {
    return cached(riverLoaderService.loadRiverNames());
  }

  @GetMapping("/lakes")
  public ResponseEntity<List<LocatedPlaceName>> lakes() {
    return cached(lakeLoaderService.loadLakeNames());
  }

  private static <T> ResponseEntity<List<T>> cached(List<T> names) {
    return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(names);
  }
}
