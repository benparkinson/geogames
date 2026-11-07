package com.parkinson.benjamin.geogames.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.GameType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class GameTypeResponseTest {

  @Test
  void describesEveryGameType() {
    for (GameType type : GameType.values()) {
      GameTypeResponse response = GameTypeResponse.from(type);

      assertThat(response.name()).isEqualTo(type.name());
      assertThat(response.answerCount()).isEqualTo(type.answerCount());
    }
  }

  @Test
  void serialisesToTheNameAndAnswerCountObjectTheFrontendReads() {
    ObjectMapper objectMapper = JsonMapper.builder().build();

    assertThat(objectMapper.writeValueAsString(GameTypeResponse.from(GameType.TRIPOINT)))
        .isEqualTo("{\"name\":\"TRIPOINT\",\"answerCount\":3}");
    assertThat(objectMapper.writeValueAsString(GameTypeResponse.from(GameType.RIVERS_BY_SHAPE)))
        .isEqualTo("{\"name\":\"RIVERS_BY_SHAPE\",\"answerCount\":1}");
  }
}
