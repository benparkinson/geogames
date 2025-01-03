package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.service.CountryLoaderService;
import com.parkinson.benjamin.geogames.service.RiverLoaderService;
import com.parkinson.benjamin.geogames.service.TripointLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RestController
public class HealthController {

  private AtomicReference<Status> status = new AtomicReference<>(Status.COLD);
  private final CountryLoaderService countryLoaderService;
  private final TripointLoaderService tripointLoaderService;
  private final RiverLoaderService riverLoaderService;

  @Autowired
  public HealthController(CountryLoaderService countryLoaderService,
      TripointLoaderService tripointLoaderService, RiverLoaderService riverLoaderService) {
    this.countryLoaderService = countryLoaderService;
    this.tripointLoaderService = tripointLoaderService;
    this.riverLoaderService = riverLoaderService;
  }

  @GetMapping(path = "/health")
  public ResponseEntity<String> health() {
    if (status.get().equals(Status.WARM)) {
      return ResponseEntity.ok("Server running and warmed up!");
    } else {
      triggerWarmup();
      return ResponseEntity.status(502).body("warming up...");
    }
  }

  private void triggerWarmup() {
    if (status.get().equals(Status.WARMING_UP)) {
      System.out.println("warmup still in progress, be patient...");
      return;
    }
    CompletableFuture.runAsync(() -> {
      try {
        status.set(Status.WARMING_UP);
        System.out.println("countries");
        countryLoaderService.loadCountries();
        System.out.println("tripoints");
        tripointLoaderService.loadTripoints();
        System.out.println("rivers");
        riverLoaderService.loadRivers();
        status.set(Status.WARM);
        System.out.println("done!");
      } catch (IOException e) {
        System.out.println("warmup failed!");
        throw new RuntimeException(e);
      }
    });
  }

  private enum Status {
    COLD,
    WARMING_UP,
    WARM
  }
}
