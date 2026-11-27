package com.parkinson.benjamin.geogames.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LandingPageController.class)
class LandingPageControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void aSlugForwardsToItsStaticPage() throws Exception {
    mockMvc
        .perform(get("/play/follow-the-river"))
        .andExpect(forwardedUrl("/play/follow-the-river.html"));
  }

  @Test
  void anythingButASlugIsNotFound() throws Exception {
    mockMvc.perform(get("/play/Not_A_Slug")).andExpect(status().isNotFound());
  }
}
