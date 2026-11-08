import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { answerPathOptions, SATELLITE_TILE_URL } from "../../map/constants";
import type { MapProps } from "../../map/MapProps";
import { drawnLayers, drawnTileUrls, fakeMap } from "../../test/leafletMock";
import { makeRiverRoundData } from "../../test/fixtures";
import RiverShapesMap from "./RiverShapesMap";
import type { RiverRound } from "./model";

vi.mock("react-leaflet", async () => await import("../../test/leafletMock"));

function renderMap(overrides: Partial<MapProps<RiverRound>> = {}) {
  render(<RiverShapesMap data={makeRiverRoundData()} gaveUp={false} gameOver={false} hintsUsed={0} {...overrides} />);
}

beforeEach(() => vi.clearAllMocks());

describe("RiverShapesMap", () => {
  it("draws the river on a blank map while playing", () => {
    renderMap();

    expect(drawnLayers()).toHaveLength(1);
    expect(drawnLayers()[0].color).toBe(answerPathOptions(false).color);
    expect(drawnTileUrls()).toEqual([]);
  });

  it("shows the satellite imagery only once the round is over", () => {
    renderMap({ gameOver: true });

    expect(drawnTileUrls()).toEqual([SATELLITE_TILE_URL]);
  });

  it("colours the river differently when the player gave up", () => {
    renderMap({ gameOver: true, gaveUp: true });

    const colour = drawnLayers()[0].color;
    expect(colour).toBe(answerPathOptions(true).color);
    expect(colour).not.toBe(answerPathOptions(false).color);
  });

  it("fits the map to the river", () => {
    renderMap();

    expect(fakeMap.fitBounds).toHaveBeenCalledTimes(1);
  });
});
