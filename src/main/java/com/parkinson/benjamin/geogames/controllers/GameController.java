package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.dao.GameType;
import com.parkinson.benjamin.geogames.model.GameCreationResponse;
import com.parkinson.benjamin.geogames.model.GameRound;
import com.parkinson.benjamin.geogames.service.GameService;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
  public ResponseEntity<GameCreationResponse> createGame(@RequestParam GameType gameType)
      throws IOException {

    return gameService
        .createGame(gameType)
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
}
