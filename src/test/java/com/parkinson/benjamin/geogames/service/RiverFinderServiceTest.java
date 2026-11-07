package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.River;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RiverFinderServiceTest {

  private final List<River> rivers =
      IntStream.range(0, 6).mapToObj(i -> new River("River" + i, List.of(), "Asia", null)).toList();
  private final RiverLoaderService loader = mock(RiverLoaderService.class);
  private final RiverFinderService service = new RiverFinderService(loader, new Random(3));

  @BeforeEach
  void setUp() {
    when(loader.loadRivers()).thenReturn(rivers);
  }

  @Test
  void generatesRiversOfTheRiversByShapeType() {
    assertThat(service.gameType()).isSameAs(GameType.RIVERS_BY_SHAPE);
  }

  @Test
  void returnsDistinctRiversFromTheInput() {
    List<GameData> result = service.generate(3);

    assertThat(result).hasSize(3).doesNotHaveDuplicates();
    assertThat(result).isSubsetOf(rivers);
  }

  @Test
  void returnsEveryRiverWhenAskedForAll() {
    assertThat(service.generate(rivers.size())).containsExactlyInAnyOrderElementsOf(rivers);
  }

  @Test
  void returnsNothingWhenAskedForZero() {
    assertThat(service.generate(0)).isEmpty();
  }

  @Test
  void rejectsMoreRiversThanAreKnown() {
    assertThatThrownBy(() -> service.generate(7))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("7")
        .hasMessageContaining("6");
  }
}
