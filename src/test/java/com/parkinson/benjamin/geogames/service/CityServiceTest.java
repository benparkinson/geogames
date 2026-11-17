package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.City;
import com.parkinson.benjamin.geogames.model.CountryCities;
import java.util.List;
import org.junit.jupiter.api.Test;

class CityServiceTest {

  private final JsonResourceReader reader = mock(JsonResourceReader.class);

  private static CountryCities country(String name) {
    return new CountryCities(name, List.of(new City(name + " City", 1, 2, 3, true)));
  }

  @Test
  void readsTheFileOnceAndServesEveryCallFromIt() {
    when(reader.read("processed/cities.json", CountryCities[].class))
        .thenReturn(new CountryCities[] {country("A"), country("B")});
    CityService service = new CityService(reader);

    assertThat(service.loadAll()).extracting(CountryCities::country).containsExactly("A", "B");
    assertThat(service.find("B").cities()).extracting(City::name).containsExactly("B City");
    assertThat(service.find("A")).isSameAs(service.loadAll().get(0));

    verify(reader, times(1)).read("processed/cities.json", CountryCities[].class);
  }

  @Test
  void aCountryWithNoCitiesIsNull() {
    when(reader.read("processed/cities.json", CountryCities[].class))
        .thenReturn(new CountryCities[] {country("A")});

    assertThat(new CityService(reader).find("Nowhere")).isNull();
  }
}
