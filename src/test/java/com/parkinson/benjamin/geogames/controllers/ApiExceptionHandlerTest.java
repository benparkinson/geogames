package com.parkinson.benjamin.geogames.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.api.BadRequestException;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ApiExceptionHandlerTest {

  @RestController
  static class Thrower {
    @GetMapping("/bad")
    String bad() {
      throw new BadRequestException("Nope, that is wrong");
    }

    @GetMapping("/bug")
    String bug() {
      throw new IllegalArgumentException("internal detail");
    }
  }

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new Thrower())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
  }

  @Test
  void aBadRequestIsA400WithItsMessage() throws Exception {
    mvc.perform(get("/bad"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Nope, that is wrong"));
  }

  @Test
  void aPlainIllegalArgumentExceptionIsNotHandledAs400() {
    assertThatThrownBy(() -> mvc.perform(get("/bug")))
        .isInstanceOf(ServletException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class);
    assertThat(new BadRequestException("x")).isInstanceOf(IllegalArgumentException.class);
  }
}
