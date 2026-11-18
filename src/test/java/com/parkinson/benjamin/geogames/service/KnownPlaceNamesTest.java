package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.CountryName;
import com.parkinson.benjamin.geogames.model.PlaceKind;
import com.parkinson.benjamin.geogames.model.PlaceName;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnownPlaceNamesTest {

  private final KnownPlaceNames names = build();

  private static KnownPlaceNames build() {
    CountryLoaderService countries = mock(CountryLoaderService.class);
    when(countries.loadCountryNames())
        .thenReturn(
            List.of(
                new CountryName("Congo", List.of("Republic of the Congo"), 0, 0),
                new CountryName("Côte d'Ivoire", List.of("Ivory Coast"), 0, 0),
                // An alias that is another place's name must not win over that name.
                new CountryName("Guinea", List.of("Congo"), 0, 0),
                new CountryName("Chad", null, 0, 0)));
    RiverLoaderService rivers = mock(RiverLoaderService.class);
    when(rivers.loadRiverNames()).thenReturn(List.of(new PlaceName("Nile", List.of())));
    LakeLoaderService lakes = mock(LakeLoaderService.class);
    when(lakes.loadLakeNames()).thenReturn(List.of(new PlaceName("Victoria", List.of())));
    return new KnownPlaceNames(countries, rivers, lakes);
  }

  @Test
  void resolvesANameInAnyCaseAndSpelling() {
    assertThat(names.resolve(PlaceKind.COUNTRIES, "COTE D IVOIRE")).contains("Côte d'Ivoire");
  }

  @Test
  void findsACountrysCurrentNameFromAnOldName() {
    assertThat(names.currentCountryName("Ivory Coast")).contains("Côte d'Ivoire");
    assertThat(names.currentCountryName("Chad")).contains("Chad");
    assertThat(names.currentCountryName("Atlantis")).isEmpty();
  }

  @Test
  void resolvesAnAliasToItsOwnSpelling() {
    assertThat(names.resolve(PlaceKind.COUNTRIES, "ivory coast")).contains("Ivory Coast");
  }

  @Test
  void aNameWinsOverAnotherPlacesAlias() {
    assertThat(names.resolve(PlaceKind.COUNTRIES, "congo")).contains("Congo");
  }

  @Test
  void aPlaceWithNoAliasesIsFound() {
    assertThat(names.resolve(PlaceKind.COUNTRIES, "Chad")).contains("Chad");
  }

  @Test
  void eachKindHasItsOwnList() {
    assertThat(names.resolve(PlaceKind.RIVERS, "nile")).contains("Nile");
    assertThat(names.resolve(PlaceKind.COUNTRIES, "nile")).isEmpty();
    assertThat(names.resolve(PlaceKind.LAKES, "Victoria")).contains("Victoria");
  }

  @Test
  void unknownBlankAndNullGuessesAreEmpty() {
    assertThat(names.resolve(PlaceKind.COUNTRIES, "Atlantis")).isEmpty();
    assertThat(names.resolve(PlaceKind.COUNTRIES, "  ")).isEmpty();
    assertThat(names.resolve(PlaceKind.COUNTRIES, null)).isEmpty();
  }
}
