package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.api.BadRequestException;
import com.parkinson.benjamin.geogames.api.ChallengeCreationResponse;
import com.parkinson.benjamin.geogames.api.ChallengeResponse;
import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.service.challenge.ChallengeService;
import com.parkinson.benjamin.geogames.service.ratelimit.GameCreationGuard;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChallengeController {

  private final ChallengeService challenges;
  private final GameCreationGuard guard;

  public ChallengeController(ChallengeService challenges, GameCreationGuard guard) {
    this.challenges = challenges;
    this.guard = guard;
  }

  /** A challenge from a finished, non-daily game. */
  @PostMapping("/games/{gameId}/challenges")
  public ChallengeCreationResponse create(
      @PathVariable String gameId, HttpServletRequest httpRequest) {
    guard.check(httpRequest);
    return challenges.create(parse(gameId));
  }

  @GetMapping("/challenges/{id}")
  public ChallengeResponse find(@PathVariable String id) {
    return challenges.find(parse(id));
  }

  /** A new game with the challenge's rounds. Each call makes another game. */
  @PostMapping("/challenges/{id}/games")
  public GameCreationResponse createGame(@PathVariable String id, HttpServletRequest httpRequest) {
    guard.check(httpRequest);
    return challenges.createGame(parse(id));
  }

  private static UUID parse(String id) {
    try {
      return UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("id must be a UUID");
    }
  }
}
