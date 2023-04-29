package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.dao.Game;
import com.parkinson.benjamin.geogames.dao.GameRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@RepositoryRestController
public class GameController {

  private final GameRepository gameRepository;

  @Autowired
  public GameController(GameRepository gameRepository) {
    this.gameRepository = gameRepository;
  }

  @RequestMapping(method = RequestMethod.POST, path = "/games")
  public ResponseEntity<Game> createGame(@RequestParam(required = false) Optional<Integer> length,
      @RequestParam String gameType,
      PersistentEntityResourceAssembler assembler) {
    Game newGame = new Game(gameType);
    Game saved = gameRepository.save(newGame);
    return new ResponseEntity(assembler.toFullResource(saved), HttpStatus.CREATED);
  }
}
