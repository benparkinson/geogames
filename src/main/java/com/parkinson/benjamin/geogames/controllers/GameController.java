package com.parkinson.benjamin.geogames.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkinson.benjamin.geogames.dao.Game;
import com.parkinson.benjamin.geogames.dao.GameData;
import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.dao.GameRound;
import com.parkinson.benjamin.geogames.dao.GameType;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.service.CountryLoaderService;
import com.parkinson.benjamin.geogames.service.RiverFinderService;
import com.parkinson.benjamin.geogames.service.RiverLoaderService;
import com.parkinson.benjamin.geogames.service.TripointFinderService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@RepositoryRestController
public class GameController {

  private final GameRepository gameRepository;
  private final CountryLoaderService countryLoaderService;

  private final TripointFinderService tripointFinderService;


  private final RiverLoaderService riverLoaderService;
  private final RiverFinderService riverFinderService;
  private final ObjectMapper objectMapper;

  @Autowired
  public GameController(GameRepository gameRepository, CountryLoaderService countryLoaderService,
      TripointFinderService tripointFinderService, RiverLoaderService riverLoaderService,
      RiverFinderService riverFinderService, ObjectMapper objectMapper) {
    this.gameRepository = gameRepository;
    this.countryLoaderService = countryLoaderService;
    this.tripointFinderService = tripointFinderService;
    this.riverLoaderService = riverLoaderService;
    this.riverFinderService = riverFinderService;
    this.objectMapper = objectMapper;
  }

  @RequestMapping(method = RequestMethod.POST, path = "/games")
  @CrossOrigin(origins = "http://localhost:3000")
  public ResponseEntity<GameCreationResponse> createGame(@RequestParam GameType gameType,
      PersistentEntityResourceAssembler assembler) throws IOException {

    if (gameType == GameType.TRIPOINT) {

      List<Country> countries = countryLoaderService.loadCountries();
      List<GameData> randomTripoints = tripointFinderService.findRandomTripoints(countries, 5);

      List<GameRound> gameRounds = new ArrayList<>(randomTripoints.size());
      for (int i = 0; i < randomTripoints.size(); i++) {
        gameRounds.add(
            new GameRound(i, objectMapper.writeValueAsString(randomTripoints.get(i))));
      }

      Game newGame = new Game(gameType, gameRounds);
      Game saved = gameRepository.save(newGame);
      PersistentEntityResource fullResource = assembler.toFullResource(saved);
      var self = fullResource.getLink("self");

      return ResponseEntity.of(self.map(link -> new GameCreationResponse(link, saved.getId())));
    }

    if (gameType == GameType.RIVERS_BY_SHAPE) {

      List<River> rivers = riverLoaderService.loadRivers();
      List<River> randomRivers = riverFinderService.findRandomRivers(rivers, 5);
      List<GameRound> gameRounds = new ArrayList<>(randomRivers.size());

      for (int i = 0; i < randomRivers.size(); i++) {
        gameRounds.add(
            new GameRound(i, objectMapper.writeValueAsString(randomRivers.get(i))));
      }

      Game newGame = new Game(gameType, gameRounds);
      Game saved = gameRepository.save(newGame);
      PersistentEntityResource fullResource = assembler.toFullResource(saved);
      var self = fullResource.getLink("self");

      return ResponseEntity.of(self.map(link -> new GameCreationResponse(link, saved.getId())));
    }

    return ResponseEntity.badRequest().build();
  }

  @RequestMapping(method = RequestMethod.GET, path = "/games/{gameId}")
  @CrossOrigin(origins = "http://localhost:3000")
  public ResponseEntity<Game> getGame(@PathVariable long gameId,
      @RequestParam Optional<Integer> round) {
    Optional<Game> game = gameRepository.findById(gameId);
    Optional<Game> gameWithRound = game.map(g -> new Game(g.getName(),
        g.getRounds().stream().filter(r -> r.getIndex() == round.orElse(0)).toList()));

    return ResponseEntity.of(gameWithRound);
  }
}
