package com.parkinson.benjamin.geogames.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.model.LocatedPlaceName;
import com.parkinson.benjamin.geogames.service.CountryNameService;
import com.parkinson.benjamin.geogames.service.LakeLoaderService;
import com.parkinson.benjamin.geogames.service.RiverLoaderService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlaceNameController.class)
class PlaceNameControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CountryNameService countryNameService;
  @MockitoBean private RiverLoaderService riverLoaderService;
  @MockitoBean private LakeLoaderService lakeLoaderService;

  @Test
  void returnsCountryNames() throws Exception {
    when(countryNameService.loadCountryNames())
        .thenReturn(
            List.of(
                new LocatedPlaceName("France", List.of(), 46.6, 2.5),
                new LocatedPlaceName("Netherlands", List.of("Holland"), 52.1, 5.3)));

    var result =
        mockMvc
            .perform(get("/api/countries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("France"))
            .andExpect(jsonPath("$[0].additionalNames.length()").value(0))
            .andExpect(jsonPath("$[0].lat").value(46.6))
            .andExpect(jsonPath("$[0].lon").value(2.5))
            .andExpect(jsonPath("$[1].name").value("Netherlands"))
            .andExpect(jsonPath("$[1].additionalNames[0]").value("Holland"))
            .andReturn();

    assertThat(result.getResponse().getHeader("Cache-Control")).isEqualTo("no-cache");
  }

  @Test
  void loadingFailureEscapesTheController() {
    when(countryNameService.loadCountryNames())
        .thenThrow(new UncheckedIOException(new IOException("disk")));

    // Data that won't load is a packaging bug, so it is meant to be a 500, not a 400.
    assertThatThrownBy(() -> mockMvc.perform(get("/api/countries")))
        .hasRootCauseInstanceOf(IOException.class)
        .hasRootCauseMessage("disk");
  }

  @Test
  void returnsRiverNames() throws Exception {
    when(riverLoaderService.loadRiverNames())
        .thenReturn(
            List.of(
                new LocatedPlaceName("Danube", List.of("Donau"), 45.5, 25.5),
                new LocatedPlaceName("Nile", List.of(), 26.1, 31.2)));

    var result =
        mockMvc
            .perform(get("/api/rivers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Danube"))
            .andExpect(jsonPath("$[0].additionalNames[0]").value("Donau"))
            .andExpect(jsonPath("$[0].lat").value(45.5))
            .andExpect(jsonPath("$[0].lon").value(25.5))
            .andExpect(jsonPath("$[1].name").value("Nile"))
            .andExpect(jsonPath("$[1].additionalNames.length()").value(0))
            .andReturn();

    assertThat(result.getResponse().getHeader("Cache-Control")).isEqualTo("no-cache");
  }

  @Test
  void riverLoadingFailureEscapesTheController() {
    when(riverLoaderService.loadRiverNames())
        .thenThrow(new UncheckedIOException(new IOException("disk")));

    assertThatThrownBy(() -> mockMvc.perform(get("/api/rivers")))
        .hasRootCauseInstanceOf(IOException.class)
        .hasRootCauseMessage("disk");
  }

  @Test
  void returnsLakeNames() throws Exception {
    when(lakeLoaderService.loadLakeNames())
        .thenReturn(
            List.of(
                new LocatedPlaceName("Lake Baikal", List.of("Baikal"), 53.5, 108.2),
                new LocatedPlaceName("Titicaca", List.of(), -15.9, -69.4)));

    var result =
        mockMvc
            .perform(get("/api/lakes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Lake Baikal"))
            .andExpect(jsonPath("$[0].additionalNames[0]").value("Baikal"))
            .andExpect(jsonPath("$[0].lat").value(53.5))
            .andExpect(jsonPath("$[0].lon").value(108.2))
            .andExpect(jsonPath("$[1].additionalNames.length()").value(0))
            .andReturn();

    assertThat(result.getResponse().getHeader("Cache-Control")).isEqualTo("no-cache");
  }
}
