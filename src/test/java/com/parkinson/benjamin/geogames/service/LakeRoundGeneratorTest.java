package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.Lake;
import com.parkinson.benjamin.geogames.model.LakeRound;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class LakeRoundGeneratorTest {

  private final List<Lake> lakes =
      IntStream.range(0, 6)
          .mapToObj(i -> new Lake("Q" + i, "Lake" + i, List.of(), "Asia", null, List.of()))
          .toList();
  private final LakeLoaderService loader = mock(LakeLoaderService.class);
  private LakeRoundGenerator generator;

  @BeforeEach
  void setUp() {
    when(loader.loadLakes()).thenReturn(lakes);
    generator = new LakeRoundGenerator(loader, new Random(3));
  }

  @Test
  void generatesLakeShapeRounds() {
    assertThat(generator.gameType()).isSameAs(GameType.LAKES_BY_SHAPE);
    assertThat(generator.generate(3, Set.of()))
        .hasSize(3)
        .doesNotHaveDuplicates()
        .allSatisfy(
            data ->
                assertThat(data)
                    .isInstanceOfSatisfying(
                        LakeRound.class,
                        round -> {
                          assertThat(round.gameType()).isSameAs(GameType.LAKES_BY_SHAPE);
                          assertThat(lakes).contains(round.lake());
                        }));
  }

  @Test
  void skipsExcludedSubjects() {
    Set<String> excluded = Set.of("lake:Q0", "lake:Q1", "lake:Q2");

    assertThat(generator.generate(3, excluded))
        .extracting(GameData::subjectId)
        .containsExactlyInAnyOrder("lake:Q3", "lake:Q4", "lake:Q5");
    assertThatThrownBy(() -> generator.generate(4, excluded))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsMoreLakesThanAreKnown() {
    assertThatThrownBy(() -> generator.generate(7, Set.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("7")
        .hasMessageContaining("6");
  }

  @Test
  void rebuildGivesBackEveryGeneratedRound() {
    ObjectMapper mapper = JsonMapper.builder().build();

    for (GameData round : generator.generate(lakes.size(), Set.of())) {
      GameData rebuilt = generator.rebuild(round.roundKey());

      assertThat(rebuilt).isEqualTo(round);
      assertThat(mapper.writeValueAsString(rebuilt)).isEqualTo(mapper.writeValueAsString(round));
    }
  }

  @Test
  void rebuildRejectsUnknownKeysAndWrongPrefixes() {
    assertThatThrownBy(() -> generator.rebuild("lake:Q99"))
        .isInstanceOf(RoundDataMissingException.class);
    assertThatThrownBy(() -> generator.rebuild("river:Q1"))
        .isInstanceOf(RoundDataMissingException.class);
  }
}
