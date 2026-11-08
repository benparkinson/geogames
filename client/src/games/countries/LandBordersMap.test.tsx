import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { answerPathOptions, clueLineOptions, clueOutlineOptions, SATELLITE_TILE_URL } from "../../map/constants";
import type { MapProps } from "../../map/MapProps";
import { drawnLayers, drawnTileUrls } from "../../test/leafletMock";
import { makeCountryShapeRoundData } from "../../test/fixtures";
import LandBordersMap from "./LandBordersMap";
import type { CountryShapeRound } from "./model";

vi.mock("react-leaflet", async () => await import("../../test/leafletMock"));

// In the fixture the layers start at: outline 1,1; coast 2,2; land borders 3,3; neighbours 4,4 and 5,5.
const OUTLINE = "1,1";
const COAST = "2,2";
const BORDERS = "3,3";
const NEIGHBOURS = ["4,4", "5,5"];

function renderMap(overrides: Partial<MapProps<CountryShapeRound>> = {}) {
  render(<LandBordersMap data={makeCountryShapeRoundData()} gaveUp={false} gameOver={false} hintsUsed={0} {...overrides} />);
}

const points = () => drawnLayers().map((layer) => layer.point);

beforeEach(() => vi.clearAllMocks());

describe("LandBordersMap", () => {
  it("draws only the land borders on a blank map while playing", () => {
    renderMap();

    expect(drawnLayers()).toHaveLength(1);
    expect(drawnLayers()[0]).toMatchObject({ point: BORDERS, color: clueLineOptions().color, dashed: false });
    expect(drawnTileUrls()).toEqual([]);
  });

  it("does not draw the country's outline or its coast while playing without the hint", () => {
    renderMap({ hintsUsed: 1 });

    expect(points()).not.toContain(OUTLINE);
    expect(points()).not.toContain(COAST);
  });

  it("never draws the country's outline while playing, whatever the hints", () => {
    renderMap({ hintsUsed: 3 });

    expect(points()).not.toContain(OUTLINE);
  });

  it("adds the neighbours' outlines from the first hint", () => {
    renderMap({ hintsUsed: 1 });

    expect(points()).toEqual(expect.arrayContaining(NEIGHBOURS));
    const neighbour = drawnLayers().find((layer) => layer.point === NEIGHBOURS[0]);
    expect(neighbour?.color).toBe(clueOutlineOptions().color);
  });

  it("adds the coastline as a dashed hint line at the second hint", () => {
    renderMap({ hintsUsed: 2 });

    const coast = drawnLayers().find((layer) => layer.point === COAST);
    expect(coast?.dashed).toBe(true);
  });

  it("shows the neighbours, the country's outline and the satellite imagery once the round is over", () => {
    renderMap({ gameOver: true });

    expect(points()).toEqual([...NEIGHBOURS, OUTLINE]);
    expect(drawnLayers()[2].color).toBe(answerPathOptions(false).color);
    expect(drawnTileUrls()).toEqual([SATELLITE_TILE_URL]);
  });

  it("colours the outline differently when the player gave up", () => {
    renderMap({ gameOver: true, gaveUp: true });

    const outline = drawnLayers().find((layer) => layer.point === OUTLINE);
    expect(outline?.color).toBe(answerPathOptions(true).color);
  });
});
