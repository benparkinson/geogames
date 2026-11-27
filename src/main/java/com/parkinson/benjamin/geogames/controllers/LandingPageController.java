package com.parkinson.benjamin.geogames.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Lets a game's landing page, /play/{slug}, load directly from the jar: the static export writes it as
 * play/{slug}.html. A controller because a view controller (see WebConfig) can't take a path variable.
 */
@Controller
public class LandingPageController {

  /** The pattern keeps anything but a slug out of the forwarded path: it is a 404. */
  @GetMapping("/play/{slug:[a-z-]+}")
  public String landingPage(@PathVariable String slug) {
    return "forward:/play/" + slug + ".html";
  }
}
