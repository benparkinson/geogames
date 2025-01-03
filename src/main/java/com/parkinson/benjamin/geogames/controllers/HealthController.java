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
  private final TripointLoaderService tripointLoaderService;
  private final RiverLoaderService riverLoaderService;

  @Autowired
  public HealthController(
      TripointLoaderService tripointLoaderService, RiverLoaderService riverLoaderService) {
    this.tripointLoaderService = tripointLoaderService;
    this.riverLoaderService = riverLoaderService;
  }

  @GetMapping(path = "/health")
  public ResponseEntity<String> health() {
    if (status.get().equals(Status.WARM)) {
      return ResponseEntity.ok("Server running and warmed up!");
    } else {
      triggerWarmup();
      return ResponseEntity.status(502).body("Warming up...");
    }
  }

  private void triggerWarmup() {
    if (status.get().equals(Status.WARMING_UP)) {
      return;
    }
    CompletableFuture.runAsync(() -> {
      try {
        status.set(Status.WARMING_UP);
        tripointLoaderService.loadTripoints();
        riverLoaderService.loadRivers();
        status.set(Status.WARM);
      } catch (IOException e) {
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
