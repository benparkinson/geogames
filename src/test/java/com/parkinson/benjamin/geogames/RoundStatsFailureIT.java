package com.parkinson.benjamin.geogames;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.parkinson.benjamin.geogames.service.stats.RoundStatsStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** A failing statistics store must never break answering a round. */
@SpringBootTest
@AutoConfigureMockMvc
class RoundStatsFailureIT {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private RoundStatsStore store;

  @Test
  void answeringStillWorksWhenTheStoreThrows() throws Exception {
    doThrow(new IllegalStateException("db down")).when(store).insert(any());
    String body =
        mockMvc
            .perform(
                post("/api/games")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"gameType\":\"RIVERS_BY_SHAPE\",\"numberOfRounds\":1}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = JsonPath.read(body, "$.id");

    mockMvc
        .perform(
            post("/api/games/%s/rounds/0/answers".formatted(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"GAVE_UP\",\"hintsUsed\":0}"))
        .andExpect(status().isOk());

    verify(store, timeout(10_000)).insert(any());
  }
}
