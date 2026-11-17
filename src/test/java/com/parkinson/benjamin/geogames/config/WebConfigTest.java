package com.parkinson.benjamin.geogames.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;

import com.parkinson.benjamin.geogames.controllers.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
class WebConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void privacyForwardsToTheStaticPage() throws Exception {
    mockMvc.perform(get("/privacy")).andExpect(forwardedUrl("/privacy.html"));
  }
}
