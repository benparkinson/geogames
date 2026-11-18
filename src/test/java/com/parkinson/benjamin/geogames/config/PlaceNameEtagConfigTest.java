package com.parkinson.benjamin.geogames.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.controllers.PlaceNameController;
import com.parkinson.benjamin.geogames.model.PlaceName;
import com.parkinson.benjamin.geogames.service.CountryNameService;
import com.parkinson.benjamin.geogames.service.LakeLoaderService;
import com.parkinson.benjamin.geogames.service.RiverLoaderService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PlaceNameEtagConfigTest {

  private final LakeLoaderService lakes = Mockito.mock(LakeLoaderService.class);
  private final MockMvc mockMvc =
      MockMvcBuilders.standaloneSetup(
              new PlaceNameController(
                  Mockito.mock(CountryNameService.class),
                  Mockito.mock(RiverLoaderService.class),
                  lakes))
          .addFilter(new PlaceNameEtagConfig().placeNameEtagFilter().getFilter(), "/api/lakes")
          .build();

  @Test
  void anUnchangedListRevalidatesWithA304AndAChangedOneDoesNot() throws Exception {
    Mockito.when(lakes.loadLakeNames()).thenReturn(List.of(new PlaceName("Baikal", List.of())));
    String etag =
        mockMvc
            .perform(get("/api/lakes"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader(HttpHeaders.ETAG);
    assertThat(etag).isNotBlank();

    mockMvc
        .perform(get("/api/lakes").header(HttpHeaders.IF_NONE_MATCH, etag))
        .andExpect(status().isNotModified());

    Mockito.when(lakes.loadLakeNames()).thenReturn(List.of(new PlaceName("Superior", List.of())));
    mockMvc
        .perform(get("/api/lakes").header(HttpHeaders.IF_NONE_MATCH, etag))
        .andExpect(status().isOk());
  }
}
