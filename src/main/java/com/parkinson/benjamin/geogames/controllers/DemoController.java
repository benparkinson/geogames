package com.parkinson.benjamin.geogames.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

  @GetMapping(path = "/api/data")
  @CrossOrigin(origins = "http://localhost:3000")
  public String data() {
    return "here's your data!";
  }
}
