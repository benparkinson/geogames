package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.River;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

class RiverLoaderServiceTest {

  @Test
  void everyRiverFileHasCompleteAdditionalData() throws IOException {
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      context.refresh();
      // TODO: a river file with no entry in additionalData.json causes a
      // NullPointerException in loadRivers(), so this test doubles as a data integrity guard.
      List<River> rivers = new RiverLoaderService(context).loadRivers();

      assertThat(rivers).isNotEmpty();
      assertThat(rivers.stream().map(River::name).toList()).doesNotHaveDuplicates();
      for (River river : rivers) {
        assertThat(river.name()).as("name").isNotBlank();
        assertThat(river.additionalNames()).as("additionalNames of " + river.name()).isNotNull();
        assertThat(river.continent()).as("continent of " + river.name()).isNotBlank();
        assertThat(river.geoData().geometry().type())
            .as("geometry of " + river.name())
            .isEqualTo("MultiLineString");
      }
    }
  }
}
