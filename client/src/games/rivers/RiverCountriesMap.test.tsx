import { render } from "@testing-library/react";
import L from "leaflet";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { answerPathOptions, clueOutlineOptions, RELIEF_TILE_URL } from "../../map/constants";
import type { MapProps } from "../../map/MapProps";
import { drawnLayers, drawnMarkers, drawnTileUrls, fakeMap } from "../../test/leafletMock";
import { makeCountryShapeData, makeRiverRoundData } from "../../test/fixtures";
import RiverCountriesMap from "./RiverCountriesMap";
import type { RiverRound } from "./model";

vi.mock("react-leaflet", async () => await import("../../test/leafletMock"));

/** Two countries, whose outlines start at "1,1". The river's line starts at "0,0". */
function roundData(focus: RiverRound["focus"] = null): RiverRound {
  const outline = makeCountryShapeData().outline;
  return makeRiverRoundData({
    countries: [
      { name: "Austria", additionalNames: [], geoData: outline },
      { name: "Hungary", additionalNames: [], geoData: outline },
    ],
    focus,
  });
}

function renderMap(overrides: Partial<MapProps<RiverRound>> = {}) {
  render(<RiverCountriesMap data={roundData()} gaveUp={false} gameOver={false} hintsUsed={0} {...overrides} />);
}

const RIVER_POINT = "0,0";

beforeEach(() => vi.clearAllMocks());

describe("RiverCountriesMap", () => {
  it("draws the outline of each country on a blank map while playing", () => {
    renderMap();

    const layers = drawnLayers();
    expect(layers).toHaveLength(2);
    expect(layers.every((layer) => layer.color === clueOutlineOptions().color)).toBe(true);
    expect(drawnTileUrls()).toEqual([]);
  });

  it("does not draw the river or its ends while playing without hints", () => {
    renderMap();

    expect(drawnLayers().map((layer) => layer.point)).not.toContain(RIVER_POINT);
    expect(drawnMarkers()).toEqual([]);
  });

  it("does not draw the river, whatever the hints", () => {
    renderMap({ hintsUsed: 3 });

    expect(drawnLayers().map((layer) => layer.point)).not.toContain(RIVER_POINT);
  });

  it("marks the source and mouth from the first hint", () => {
    renderMap({ hintsUsed: 1 });

    expect(drawnMarkers()).toHaveLength(2);
  });

  it("draws the river, the end pins and the relief map once the round is over", () => {
    renderMap({ gameOver: true });

    const river = drawnLayers().filter((layer) => layer.point === RIVER_POINT);
    expect(river).toHaveLength(1);
    expect(river[0].color).toBe(answerPathOptions(false).color);
    expect(drawnMarkers()).toHaveLength(2);
    expect(drawnTileUrls()).toEqual([RELIEF_TILE_URL]);
  });

  it("colours the river differently when the player gave up", () => {
    renderMap({ gameOver: true, gaveUp: true });

    const river = drawnLayers().find((layer) => layer.point === RIVER_POINT);
    expect(river?.color).toBe(answerPathOptions(true).color);
  });

  it("fits the map to the server's box when the round has one", () => {
    renderMap({ data: roundData({ west: 9, south: 43, east: 30, north: 50 }) });

    expect(fakeMap.fitBounds).toHaveBeenCalledTimes(1);
    expect(fakeMap.fitBounds.mock.calls[0][0]).toEqual([
      [43, 9],
      [50, 30],
    ]);
  });

  it("fits the map to the river when the round has no box", () => {
    renderMap();

    expect(fakeMap.fitBounds).toHaveBeenCalledTimes(1);
    const target = fakeMap.fitBounds.mock.calls[0][0] as L.LatLngBounds;
    expect(target.toBBoxString()).toBe("0,0,1,1");
  });
});
