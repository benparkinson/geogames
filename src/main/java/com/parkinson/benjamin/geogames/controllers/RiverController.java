package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.service.CountryLoaderService;
import com.parkinson.benjamin.geogames.service.RiverFinderService;
import com.parkinson.benjamin.geogames.service.RiverLoaderService;
import com.parkinson.benjamin.geogames.service.TripointFinderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
public class RiverController {

  @Autowired
  private RiverLoaderService riverLoaderService;

  @Autowired
  private RiverFinderService riverFinderService;

  @GetMapping("/api/river-shape")
  @CrossOrigin(origins = "http://localhost:3000")
  public River findRandomRiverShape() throws IOException {
    List<River> rivers = riverLoaderService.loadRivers();
    return riverFinderService.findRiver(rivers);
  }

}
