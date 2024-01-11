package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.model.GameCreationRequest;
import com.parkinson.benjamin.geogames.model.GameCreationResponse;
import com.parkinson.benjamin.geogames.model.GameRound;
import com.parkinson.benjamin.geogames.model.GameRoundAnswerRequest;
import com.parkinson.benjamin.geogames.service.GameService;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class GameController {

  private final GameService gameService;

  @Autowired
  public GameController(GameService gameService) {
    this.gameService = gameService;
  }

  @RequestMapping(method = RequestMethod.POST, path = "api/games")
  public ResponseEntity<GameCreationResponse> createGame(@RequestBody GameCreationRequest request)
      throws IOException {

    return gameService
        .createGame(request.gameType(), request.numberOfRounds())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.badRequest().build());
  }

  @RequestMapping(method = RequestMethod.GET, path = "api/games/{gameId}/rounds/{round}")
  public ResponseEntity<GameRound> getGameRound(
      @PathVariable long gameId, @PathVariable int round) {
    return gameService
        .getGameRound(gameId, round)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(method = RequestMethod.POST, path = "api/games/{gameId}/rounds/{round}/answers")
  public ResponseEntity<GameRound> submitAnswer(
      @PathVariable long gameId,
      @PathVariable int round,
      @RequestBody GameRoundAnswerRequest answer) {
    return gameService
        .submitAnswer(gameId, round, answer.answerState())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
