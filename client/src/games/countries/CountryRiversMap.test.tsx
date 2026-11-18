import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { answerPathOptions, clueLineOptions, lakeFillOptions, minorRiverOptions, RELIEF_TILE_URL } from "../../map/constants";
import type { MapProps } from "../../map/MapProps";
import { drawnLayers, drawnTileUrls, fakeMap } from "../../test/leafletMock";
import { makeCountryRiversRoundData, makeCountryShapeData } from "../../test/fixtures";
import CountryRiversMap from "./CountryRiversMap";
import type { CountryRiversRound } from "./model";

vi.mock("react-leaflet", async () => await import("../../test/leafletMock"));

// In the fixture the layers start at: outline 1,1; coast 2,2; land borders 3,3; major rivers 10,10;
// minor rivers 11,11; lakes 12,12.
const OUTLINE = "1,1";
const COAST = "2,2";
const MAJOR = "10,10";
const MINOR = "11,11";
const LAKES = "12,12";

function renderMap(overrides: Partial<MapProps<CountryRiversRound>> = {}, data = makeCountryRiversRoundData()) {
  render(<CountryRiversMap data={data} gaveUp={false} gameOver={false} hintsUsed={0} {...overrides} />);
}

const points = () => drawnLayers().map((layer) => layer.point);

const emptyLines = { type: "MultiLineString", properties: {}, geometry: { type: "MultiLineString", coordinates: [] } };
const noLakes = { type: "MultiPolygon", properties: {}, geometry: { type: "MultiPolygon", coordinates: [] } };

beforeEach(() => vi.clearAllMocks());

describe("CountryRiversMap", () => {
  it("draws the minor rivers thin and the major rivers over them on a blank map while playing", () => {
    renderMap();

    expect(points()).toEqual([MINOR, MAJOR]);
    expect(drawnLayers()[0].color).toBe(minorRiverOptions().color);
    expect(drawnLayers()[1].color).toBe(clueLineOptions().color);
    expect(drawnTileUrls()).toEqual([]);
  });

  it("never draws the outline while playing", () => {
    renderMap({ hintsUsed: 3 });

    expect(points()).not.toContain(OUTLINE);
  });

  it("fits the map to the country", () => {
    renderMap();

    expect(fakeMap.fitBounds).toHaveBeenCalledTimes(1);
  });

  it("draws the lakes under the rivers at the first hint", () => {
    renderMap({ hintsUsed: 1 });

    expect(points()).toEqual([LAKES, MINOR, MAJOR]);
    expect(drawnLayers()[0].color).toBe(lakeFillOptions().color);
  });

  it("adds the coastline as a dashed line at the second hint", () => {
    renderMap({ hintsUsed: 2 });

    expect(points()).toEqual([LAKES, COAST, MINOR, MAJOR]);
    expect(drawnLayers()[1].dashed).toBe(true);
  });

  it("draws no lakes for a country without any", () => {
    renderMap({ hintsUsed: 1 }, makeCountryRiversRoundData({ lakes: noLakes as never }));

    expect(points()).toEqual([MINOR, MAJOR]);
  });

  it("draws no coastline for a country without one", () => {
    const landlocked = makeCountryRiversRoundData({
      country: makeCountryShapeData({ coastKm: 0, coast: emptyLines as never }),
    });
    renderMap({ hintsUsed: 2 }, landlocked);

    expect(points()).toEqual([LAKES, MINOR, MAJOR]);
  });

  it("shows the outline and the relief map instead of the rivers once the round is over", () => {
    renderMap({ gameOver: true, hintsUsed: 3 });

    expect(points()).toEqual([OUTLINE]);
    expect(drawnLayers()[0].color).toBe(answerPathOptions(false).color);
    expect(drawnTileUrls()).toEqual([RELIEF_TILE_URL]);
  });

  it("colours the outline differently when the player gave up", () => {
    renderMap({ gameOver: true, gaveUp: true });

    expect(drawnLayers()[0].color).toBe(answerPathOptions(true).color);
  });
});
