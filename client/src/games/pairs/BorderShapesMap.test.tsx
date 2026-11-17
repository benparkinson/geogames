import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { answerPathOptions, clueLineOptions, SATELLITE_TILE_URL } from "../../map/constants";
import type { MapProps } from "../../map/MapProps";
import { drawnLayers, drawnTileUrls, fakeMap } from "../../test/leafletMock";
import { makeCountryPairRoundData } from "../../test/fixtures";
import BorderShapesMap from "./BorderShapesMap";
import type { CountryPairRound } from "./model";

vi.mock("react-leaflet", async () => await import("../../test/leafletMock"));

// In the fixture the layers start at: Belgium outline 1,1, coast 2,2, borders 3,3; the Netherlands
// outline 4,4, coast 5,5, borders 6,6; the shared border 7,7.
const BORDER = "7,7";
const BORDERS = ["3,3", "6,6"];
const COASTS = ["2,2", "5,5"];
const OUTLINES = ["1,1", "4,4"];

function renderMap(overrides: Partial<MapProps<CountryPairRound>> = {}) {
  render(<BorderShapesMap data={makeCountryPairRoundData()} gaveUp={false} gameOver={false} hintsUsed={0} {...overrides} />);
}

const points = () => drawnLayers().map((layer) => layer.point);

beforeEach(() => vi.clearAllMocks());

describe("BorderShapesMap", () => {
  it("draws only the shared border on a blank map while playing", () => {
    renderMap();

    expect(drawnLayers()).toHaveLength(1);
    expect(drawnLayers()[0]).toMatchObject({ point: BORDER, color: clueLineOptions().color, dashed: false });
    expect(drawnTileUrls()).toEqual([]);
  });

  it("fits the map to the border at first", () => {
    renderMap();

    expect(fakeMap.fitBounds).toHaveBeenCalledTimes(1);
    expect(fakeMap.fitBounds.mock.calls[0][0]).toEqual([
      [50.7, 3.9],
      [51.5, 6.4],
    ]);
  });

  it("adds both countries' land borders under the border line at the first hint, and widens the frame", () => {
    renderMap({ hintsUsed: 1 });

    expect(points()).toEqual([...BORDERS, BORDER]);
    expect(drawnLayers().slice(0, 2).every((layer) => layer.dashed)).toBe(true);
    expect(drawnLayers()[2].dashed).toBe(false);
    expect(fakeMap.fitBounds.mock.calls[0][0]).toEqual([
      [49.5, 2.5],
      [53.6, 7.2],
    ]);
  });

  it("adds both coastlines at the second hint", () => {
    renderMap({ hintsUsed: 2 });

    expect(points()).toEqual([...BORDERS, ...COASTS, BORDER]);
  });

  it("never draws the outlines while playing", () => {
    renderMap({ hintsUsed: 3 });

    for (const outline of OUTLINES) {
      expect(points()).not.toContain(outline);
    }
  });

  it("fits the map again when the first hint is taken", () => {
    const { rerender } = render(<BorderShapesMap data={makeCountryPairRoundData()} gaveUp={false} gameOver={false} hintsUsed={0} />);
    rerender(<BorderShapesMap data={makeCountryPairRoundData()} gaveUp={false} gameOver={false} hintsUsed={1} />);

    expect(fakeMap.fitBounds).toHaveBeenCalledTimes(2);
  });

  it("shows both countries and the satellite imagery once the round is over", () => {
    renderMap({ gameOver: true });

    expect(points()).toEqual(OUTLINES);
    expect(drawnLayers()[0].color).toBe(answerPathOptions(false).color);
    expect(drawnTileUrls()).toEqual([SATELLITE_TILE_URL]);
    expect(fakeMap.fitBounds.mock.calls[0][0]).toEqual([
      [49.5, 2.5],
      [53.6, 7.2],
    ]);
  });

  it("colours the outlines differently when the player gave up", () => {
    renderMap({ gameOver: true, gaveUp: true });

    expect(drawnLayers().map((layer) => layer.color)).toEqual([answerPathOptions(true).color, answerPathOptions(true).color]);
  });
});
