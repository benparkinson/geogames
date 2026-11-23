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
@TestPropertySource(properties = "geogames.rate-limit.games-per-window=3")
class RateLimitIT {

  @Autowired private MockMvc mockMvc;

  private static MockHttpServletRequestBuilder newGame(String forwardedFor) {
    return post("/api/games")
        .header("X-Forwarded-For", forwardedFor)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"gameType\":\"TRIPOINT\",\"numberOfRounds\":1}");
  }

  @Test
  void theFourthGameFromOneAddressIsRefusedAcrossBothEndpoints() throws Exception {
    for (int i = 0; i < 3; i++) {
      mockMvc.perform(newGame("198.51.100.1")).andExpect(status().isOk());
    }

    mockMvc
        .perform(newGame("198.51.100.1"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.status").value(429))
        .andExpect(jsonPath("$.detail").exists());
    mockMvc
        .perform(
            post("/api/daily-games")
                .header("X-Forwarded-For", "198.51.100.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));

    mockMvc.perform(newGame("198.51.100.2")).andExpect(status().isOk());
  }
}
