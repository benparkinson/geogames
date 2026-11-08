package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.api.GameCreationRequest;
import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.api.GameRound;
import com.parkinson.benjamin.geogames.api.GameRoundAnswerRequest;
import com.parkinson.benjamin.geogames.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GameController {

  private final GameService gameService;

  public GameController(GameService gameService) {
    this.gameService = gameService;
  }

  @PostMapping("/games")
  public ResponseEntity<GameCreationResponse> createGame(@RequestBody GameCreationRequest request) {

    return ResponseEntity.ok(gameService.createGame(request.gameType(), request.numberOfRounds()));
  }

  @GetMapping("/games/{gameId}/rounds/{round}")
  public ResponseEntity<GameRound> getGameRound(
      @PathVariable long gameId, @PathVariable int round) {
    return gameService
        .getGameRound(gameId, round)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/games/{gameId}/rounds/{round}/answers")
  public ResponseEntity<GameRound> submitAnswer(
      @PathVariable long gameId,
      @PathVariable int round,
      @RequestBody GameRoundAnswerRequest answer) {
    return gameService
        .submitAnswer(gameId, round, answer.answerState(), answer.hintsUsedOrZero())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
