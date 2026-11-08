import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { answerPathOptions, SATELLITE_TILE_URL } from "../../map/constants";
import type { MapProps } from "../../map/MapProps";
import { drawnLayers, drawnTileUrls, fakeMap } from "../../test/leafletMock";
import { makeRiverRoundData } from "../../test/fixtures";
import RiverOnMapMap from "./RiverOnMapMap";
import type { RiverRound } from "./model";

vi.mock("react-leaflet", async () => await import("../../test/leafletMock"));

function renderMap(overrides: Partial<MapProps<RiverRound>> = {}) {
  render(<RiverOnMapMap data={makeRiverRoundData()} gaveUp={false} gameOver={false} hintsUsed={0} {...overrides} />);
}

beforeEach(() => vi.clearAllMocks());

describe("RiverOnMapMap", () => {
  it("draws the river on the satellite imagery from the start", () => {
    renderMap();

    expect(drawnTileUrls()).toEqual([SATELLITE_TILE_URL]);
    expect(drawnLayers()).toHaveLength(1);
    expect(drawnLayers()[0].color).toBe(answerPathOptions(false).color);
  });

  it("locks panning while playing and frees it when the round is over", () => {
    renderMap();

    expect(fakeMap.dragging.disable).toHaveBeenCalled();
    expect(fakeMap.dragging.enable).not.toHaveBeenCalled();
  });

  it("lets the player pan once the round is over", () => {
    renderMap({ gameOver: true });

    expect(fakeMap.dragging.enable).toHaveBeenCalled();
    expect(fakeMap.dragging.disable).not.toHaveBeenCalled();
  });

  it("colours the river differently when the player gave up", () => {
    renderMap({ gameOver: true, gaveUp: true });

    expect(drawnLayers()[0].color).toBe(answerPathOptions(true).color);
    expect(answerPathOptions(true).color).not.toBe(answerPathOptions(false).color);
  });
});
