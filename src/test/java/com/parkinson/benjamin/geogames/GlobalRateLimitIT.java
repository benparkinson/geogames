package com.parkinson.benjamin.geogames;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "geogames.rate-limit.global-games-per-minute=2")
class GlobalRateLimitIT {

  @Autowired private MockMvc mockMvc;

  private static MockHttpServletRequestBuilder newGame(String forwardedFor) {
    return post("/api/games")
        .header("X-Forwarded-For", forwardedFor)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"gameType\":\"TRIPOINT\",\"numberOfRounds\":1}");
  }

  @Test
  void theThirdGameInAMinuteFromAThirdAddressIsRefusedWithRetryAfter() throws Exception {
    mockMvc.perform(newGame("203.0.113.1")).andExpect(status().isOk());
    mockMvc.perform(newGame("203.0.113.2")).andExpect(status().isOk());

    mockMvc
        .perform(newGame("203.0.113.3"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.status").value(503));
  }
}
