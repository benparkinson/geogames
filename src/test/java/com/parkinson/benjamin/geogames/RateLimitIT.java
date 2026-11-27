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
@TestPropertySource(
    properties = {
      "geogames.rate-limit.games-per-window=3",
      "geogames.rate-limit.requests-per-window=20"
    })
class RateLimitIT {

  @Autowired private MockMvc mockMvc;

  private static MockHttpServletRequestBuilder newGame(String forwardedFor) {
    return post("/api/games")
        .header("X-Forwarded-For", forwardedFor)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"gameType\":\"TRIPOINT\",\"numberOfRounds\":1}");
  }

  private static MockHttpServletRequestBuilder get(String path, String forwardedFor) {
    return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
        .header("X-Forwarded-For", forwardedFor);
  }

  @Test
  void theRequestAfterTheLimitIsARefusedProblemDetailAndAnotherAddressIsNot() throws Exception {
    for (int i = 0; i < 20; i++) {
      mockMvc.perform(get("/api/countries", "203.0.113.1")).andExpect(status().isOk());
    }

    mockMvc
        .perform(get("/api/countries", "203.0.113.1"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.status").value(429))
        .andExpect(jsonPath("$.detail").exists());
    mockMvc.perform(get("/api/countries", "203.0.113.2")).andExpect(status().isOk());
  }

  @Test
  void theHealthCheckIsNeverLimited() throws Exception {
    for (int i = 0; i < 25; i++) {
      mockMvc.perform(get("/health", "203.0.113.3")).andExpect(status().isOk());
    }
  }

  @Test
  void addressesInOneIpv6PrefixShareTheCreationLimit() throws Exception {
    for (int i = 0; i < 3; i++) {
      mockMvc.perform(newGame("2001:db8:aa:bb::" + (i + 1))).andExpect(status().isOk());
    }

    mockMvc.perform(newGame("2001:db8:aa:bb:1:2:3:4")).andExpect(status().isTooManyRequests());
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
