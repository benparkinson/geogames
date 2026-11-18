package com.parkinson.benjamin.geogames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.service.stats.RoundStatsRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "geogames.stats.enabled=false")
@AutoConfigureMockMvc
class RoundStatsDisabledIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ApplicationContext context;

  @Test
  void thereIsNoRecorderAndAnsweringStillWorks() throws Exception {
    assertThat(context.getBeanNamesForType(RoundStatsRecorder.class)).isEmpty();
    String id = TestFixtures.createGame(mockMvc, "RIVERS_BY_SHAPE", 1);

    mockMvc
        .perform(
            post("/api/games/%s/rounds/0/answers".formatted(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"GAVE_UP\",\"hintsUsed\":0}"))
        .andExpect(status().isOk());
  }
}
