import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { answerPathOptions, clueOutlineOptions, hiddenBorderOptions, mergedShapeOptions, SATELLITE_TILE_URL } from "../../map/constants";
import type { MapProps } from "../../map/MapProps";
import { drawnLayers, drawnTileUrls, fakeMap } from "../../test/leafletMock";
import { makeCountryPairRoundData } from "../../test/fixtures";
import MergedCountriesMap from "./MergedCountriesMap";
import type { CountryPairRound } from "./model";

vi.mock("react-leaflet", async () => await import("../../test/leafletMock"));

// In the fixture the layers start at: Belgium outline 1,1; the Netherlands outline 4,4; the shared
// border 7,7; the neighbours 8,8 (Germany) and 9,9 (France).
const OUTLINES = ["1,1", "4,4"];
const BORDER = "7,7";
const NEIGHBOURS = ["8,8", "9,9"];

function renderMap(overrides: Partial<MapProps<CountryPairRound>> = {}) {
  render(<MergedCountriesMap data={makeCountryPairRoundData()} gaveUp={false} gameOver={false} hintsUsed={0} {...overrides} />);
}

const points = () => drawnLayers().map((layer) => layer.point);

beforeEach(() => vi.clearAllMocks());

describe("MergedCountriesMap", () => {
  it("draws both countries in the same colour and nothing else while playing", () => {
    renderMap();

    expect(points()).toEqual(OUTLINES);
    expect(drawnLayers().map((layer) => layer.color)).toEqual([mergedShapeOptions().color, mergedShapeOptions().color]);
    expect(drawnTileUrls()).toEqual([]);
  });

  it("fits the map to both countries", () => {
    renderMap();

    expect(fakeMap.fitBounds.mock.calls[0][0]).toEqual([
      [49.5, 2.5],
      [53.6, 7.2],
    ]);
  });

  it("adds the surrounding countries under the shape at the first hint", () => {
    renderMap({ hintsUsed: 1 });

    expect(points()).toEqual([...NEIGHBOURS, ...OUTLINES]);
    expect(drawnLayers()[0].color).toBe(clueOutlineOptions().color);
  });

  it("draws the hidden border over the shape at the second hint", () => {
    renderMap({ hintsUsed: 2 });

    expect(points()).toEqual([...NEIGHBOURS, ...OUTLINES, BORDER]);
    expect(drawnLayers()[4].color).toBe(hiddenBorderOptions().color);
  });

  it("shows the neighbours, both countries and the satellite imagery once the round is over", () => {
    renderMap({ gameOver: true });

    expect(points()).toEqual([...NEIGHBOURS, ...OUTLINES]);
    expect(drawnLayers()[2].color).toBe(answerPathOptions(false).color);
    expect(drawnTileUrls()).toEqual([SATELLITE_TILE_URL]);
  });

  it("colours the outlines differently when the player gave up", () => {
    renderMap({ gameOver: true, gaveUp: true });

    expect(drawnLayers()[2].color).toBe(answerPathOptions(true).color);
  });
});
