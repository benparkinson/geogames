package com.parkinson.benjamin.geogames;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Values and builders shared by tests. A test that doesn't care about a game's settings uses these,
 * so a new setting changes this class and not every test.
 */
public final class TestFixtures {

  /**
   * Number of countries in the autocomplete list: the files in {@code processed/country-shapes}
   * (241) less the ones in {@code CountryNameService.NOT_IN_AUTOCOMPLETE}. Update here when the
   * country data changes.
   */
  public static final int EXPECTED_COUNTRY_COUNT = 240;

  private TestFixtures() {}

  /** An unsaved {@code TRIPOINT} game with these rounds and every optional setting unknown. */
  public static GameEntity game(GameRoundEntity... rounds) {
    return game(List.of(rounds));
  }

  /** An unsaved {@code TRIPOINT} game with these rounds and every optional setting unknown. */
  public static GameEntity game(List<GameRoundEntity> rounds) {
    return new GameEntity(new ArrayList<>(rounds), "TRIPOINT", null);
  }

  /** Creates a game through the API, expects success and returns its public id. */
  public static String createGame(MockMvc mockMvc, String gameType, int rounds) throws Exception {
    return createGame(mockMvc, gameType, rounds, "");
  }

  /**
   * Creates a game through the API, expects success and returns its public id.
   *
   * @param extra more JSON members for the request, such as {@code ,"difficulty":"HARD"}
   */
  public static String createGame(MockMvc mockMvc, String gameType, int rounds, String extra)
      throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/games")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"gameType\":\"%s\",\"numberOfRounds\":%d%s}"
                            .formatted(gameType, rounds, extra)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }
}
