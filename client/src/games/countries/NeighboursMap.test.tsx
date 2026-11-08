import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { answerPathOptions, clueOutlineOptions, SATELLITE_TILE_URL } from "../../map/constants";
import type { MapProps } from "../../map/MapProps";
import { drawnLayers, drawnTileUrls, fakeMap } from "../../test/leafletMock";
import { makeCountryShapeRoundData } from "../../test/fixtures";
import NeighboursMap from "./NeighboursMap";
import type { CountryShapeRound } from "./model";

vi.mock("react-leaflet", async () => await import("../../test/leafletMock"));

// In the fixture the layers start at: outline 1,1; coast 2,2; land borders 3,3; neighbours 4,4 and 5,5.
const OUTLINE = "1,1";
const COAST = "2,2";
const BORDERS = "3,3";

function renderMap(overrides: Partial<MapProps<CountryShapeRound>> = {}) {
  render(<NeighboursMap data={makeCountryShapeRoundData()} gaveUp={false} gameOver={false} hintsUsed={0} {...overrides} />);
}

const points = () => drawnLayers().map((layer) => layer.point);

beforeEach(() => vi.clearAllMocks());

describe("NeighboursMap", () => {
  it("draws only the neighbours on a blank map while playing", () => {
    renderMap();

    expect(points()).toEqual(["4,4", "5,5"]);
    expect(drawnLayers().every((layer) => layer.color === clueOutlineOptions().color)).toBe(true);
    expect(drawnTileUrls()).toEqual([]);
  });

  it("does not draw the country's own outline while playing, whatever the hints", () => {
    renderMap({ hintsUsed: 3 });

    expect(points()).not.toContain(OUTLINE);
  });

  it("adds the coastline as a dashed hint line from the first hint", () => {
    renderMap({ hintsUsed: 1 });

    const coast = drawnLayers().find((layer) => layer.point === COAST);
    expect(coast?.dashed).toBe(true);
  });

  it("never draws the land borders", () => {
    renderMap({ hintsUsed: 3 });
    renderMap({ gameOver: true });

    expect(points()).not.toContain(BORDERS);
  });

  it("draws the country's outline and the satellite imagery once the round is over", () => {
    renderMap({ gameOver: true });

    const outline = drawnLayers().find((layer) => layer.point === OUTLINE);
    expect(outline?.color).toBe(answerPathOptions(false).color);
    expect(drawnTileUrls()).toEqual([SATELLITE_TILE_URL]);
  });

  it("drops the hint line when the round is over", () => {
    renderMap({ gameOver: true, hintsUsed: 1 });

    expect(points()).not.toContain(COAST);
  });

  it("colours the outline differently when the player gave up", () => {
    renderMap({ gameOver: true, gaveUp: true });

    const outline = drawnLayers().find((layer) => layer.point === OUTLINE);
    expect(outline?.color).toBe(answerPathOptions(true).color);
    expect(answerPathOptions(true).color).not.toBe(answerPathOptions(false).color);
  });

  it("fits the map to the country's focus box", () => {
    renderMap();

    expect(fakeMap.fitBounds).toHaveBeenCalledWith(
      [
        [50.7, 3.3],
        [53.6, 7.2],
      ],
      expect.anything(),
    );
  });
});
