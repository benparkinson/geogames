package com.parkinson.benjamin.geogames.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.model.CountryName;
import com.parkinson.benjamin.geogames.service.CountryLoaderService;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CountryController.class)
class CountryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CountryLoaderService countryLoaderService;

  @Test
  void returnsCountryNamesWithADayOfCaching() throws Exception {
    when(countryLoaderService.loadCountryNames())
        .thenReturn(
            List.of(
                new CountryName("France", List.of()),
                new CountryName("Netherlands", List.of("Holland"))));

    var result =
        mockMvc
            .perform(get("/api/countries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("France"))
            .andExpect(jsonPath("$[0].additionalNames.length()").value(0))
            .andExpect(jsonPath("$[1].name").value("Netherlands"))
            .andExpect(jsonPath("$[1].additionalNames[0]").value("Holland"))
            .andReturn();

    assertThat(result.getResponse().getHeader("Cache-Control")).contains("max-age=86400");
  }

  @Test
  void loadingFailureEscapesTheController() {
    try {
      when(countryLoaderService.loadCountryNames()).thenThrow(new IOException("disk"));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    // TODO: an IOException from the loader is not handled, so the request fails
    // (a 500 in a real server) instead of returning a descriptive error response.
    assertThatThrownBy(() -> mockMvc.perform(get("/api/countries")))
        .isInstanceOf(IOException.class)
        .hasMessage("disk");
  }
}
