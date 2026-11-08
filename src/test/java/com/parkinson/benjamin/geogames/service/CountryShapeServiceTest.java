package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkinson.benjamin.geogames.model.CountryShape;
import java.io.UncheckedIOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class CountryShapeServiceTest {

  private final CountryShapeService service =
      new CountryShapeService(new JsonResourceReader(new PathMatchingResourcePatternResolver()));

  @Test
  void loadsOneShapeByName() {
    CountryShape shape = service.loadShape("Switzerland");

    assertThat(shape.name()).isEqualTo("Switzerland");
    assertThat(shape.coastKm()).isZero();
    assertThat(shape.neighbours()).hasSize(5);
    assertThat(shape.outline().geometry().type()).isIn("Polygon", "MultiPolygon");
    assertThat(shape.coast().geometry().type()).isEqualTo("MultiLineString");
    assertThat(shape.coast().geometry().coordinates()).isEmpty();
    assertThat(shape.landBorders().geometry().coordinates()).isNotEmpty();
  }

  @Test
  void loadsNamesWithSpacesAndFullStops() {
    assertThat(service.loadShape("United States of America").name())
        .isEqualTo("United States of America");
  }

  @Test
  void loadsEveryShape() {
    List<CountryShape> shapes = service.loadShapes();

    assertThat(shapes).hasSize(241);
    assertThat(shapes).extracting(CountryShape::name).doesNotContain("Antarctica");
  }

  @Test
  void loadShapeReturnsTheInstanceInLoadShapes() {
    CountryShape one = service.loadShapes().get(10);

    assertThat(service.loadShape(one.name())).isSameAs(one);
  }

  @Test
  void readsTheFilesOnlyOnce() {
    JsonResourceReader reader =
        org.mockito.Mockito.spy(new JsonResourceReader(new PathMatchingResourcePatternResolver()));
    CountryShapeService once = new CountryShapeService(reader);

    once.loadShape("Switzerland");
    once.loadShapes();
    once.loadShape("France");

    org.mockito.Mockito.verify(reader, org.mockito.Mockito.times(1))
        .readAll(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.eq(CountryShape.class));
  }

  @Test
  void anUnknownCountryIsAPackagingError() {
    assertThatThrownBy(() -> service.loadShape("Atlantis"))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessageContaining("Atlantis");
  }
}
