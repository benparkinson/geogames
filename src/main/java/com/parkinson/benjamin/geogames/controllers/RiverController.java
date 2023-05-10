package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.service.RiverFinderService;
import com.parkinson.benjamin.geogames.service.RiverLoaderService;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RiverController {

  private final RiverLoaderService riverLoaderService;

  private final RiverFinderService riverFinderService;

  @Autowired
  public RiverController(RiverLoaderService riverLoaderService,
      RiverFinderService riverFinderService) {
    this.riverLoaderService = riverLoaderService;
    this.riverFinderService = riverFinderService;
  }

  @GetMapping("/api/river-shape")
  @CrossOrigin(origins = "http://localhost:3000")
  public River findRandomRiverShape() throws IOException {
    List<River> rivers = riverLoaderService.loadRivers();
    return riverFinderService.findRiver(rivers);
  }

}
