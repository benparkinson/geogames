package com.parkinson.benjamin.geogames.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class BoundsTest {

  private static final Bounds A = new Bounds(0, 0, 4, 2);
  private static final Bounds B = new Bounds(2, -1, 6, 1);

  @Test
  void unionHoldsBoth() {
    assertThat(A.union(B)).isEqualTo(new Bounds(0, -1, 6, 2));
  }

  @Test
  void intersectIsTheSharedPart() {
    assertThat(A.intersect(B)).isEqualTo(new Bounds(2, 0, 4, 1));
  }

  @Test
  void expandGrowsEachSideByAShareOfTheSize() {
    assertThat(A.expand(0.25)).isEqualTo(new Bounds(-1, -0.5, 5, 2.5));
  }

  @Test
  void areaIsWidthTimesHeight() {
    assertThat(A.area()).isEqualTo(8);
  }

  @Test
  void ofCoversEveryLineOfAMultiLineString() {
    List<List<List<Double>>> lines =
        List.of(
            List.of(List.of(1.0, 5.0), List.of(2.0, 6.0)),
            List.of(List.of(-3.0, 4.0), List.of(0.0, 9.0)));
    GeoData data =
        new GeoData("Feature", new GeoProperties("x"), new GeoGeometry("MultiLineString", lines));

    assertThat(Bounds.of(data)).isEqualTo(new Bounds(-3, 4, 2, 9));
  }
}
