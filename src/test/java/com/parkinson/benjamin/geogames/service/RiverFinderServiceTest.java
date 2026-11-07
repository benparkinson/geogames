package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkinson.benjamin.geogames.dao.GameData;
import com.parkinson.benjamin.geogames.model.River;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RiverFinderServiceTest {

  private final RiverFinderService service = new RiverFinderService();
  private final List<River> rivers =
      IntStream.range(0, 6).mapToObj(i -> new River("River" + i, List.of(), "Asia", null)).toList();

  @Test
  void returnsDistinctRiversFromTheInput() {
    List<GameData> result = service.findRandomRivers(rivers, 3);

    assertThat(result).hasSize(3).doesNotHaveDuplicates();
    assertThat(result).isSubsetOf(rivers);
  }

  @Test
  void returnsEveryRiverWhenAskedForAll() {
    assertThat(service.findRandomRivers(rivers, rivers.size()))
        .containsExactlyInAnyOrderElementsOf(rivers);
  }

  @Test
  void returnsNothingWhenAskedForZero() {
    assertThat(service.findRandomRivers(rivers, 0)).isEmpty();
  }

  @Test
  void rejectsMoreRiversThanAreKnown() {
    assertThatThrownBy(() -> service.findRandomRivers(rivers, 7))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("7")
        .hasMessageContaining("6");
  }
}
