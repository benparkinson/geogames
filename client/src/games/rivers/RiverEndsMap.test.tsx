import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { answerPathOptions } from "../../map/constants";
import type { MapProps } from "../../map/MapProps";
import { drawnLayers, drawnMarkers, fakeMap } from "../../test/leafletMock";
import { makeRiverRoundData } from "../../test/fixtures";
import RiverEndsMap from "./RiverEndsMap";
import type { RiverRound } from "./model";

vi.mock("react-leaflet", async () => await import("../../test/leafletMock"));

function renderMap(overrides: Partial<MapProps<RiverRound>> = {}) {
  render(<RiverEndsMap data={makeRiverRoundData()} gaveUp={false} gameOver={false} hintsUsed={0} {...overrides} />);
}

beforeEach(() => vi.clearAllMocks());

describe("RiverEndsMap", () => {
  it("does not draw the river while the round is being played", () => {
    renderMap();

    expect(drawnLayers()).toEqual([]);
  });

  it("does not draw the river however many hints were used", () => {
    renderMap({ hintsUsed: 3 });

    expect(drawnLayers()).toEqual([]);
  });

  it("marks the source and the mouth from the start", () => {
    renderMap();

    expect(drawnMarkers()).toEqual([
      [48.0, 8.1],
      [45.2, 29.7],
    ]);
  });

  it("draws the river once the round is over", () => {
    renderMap({ gameOver: true });

    expect(drawnLayers()).toHaveLength(1);
    expect(drawnLayers()[0].color).toBe(answerPathOptions(false).color);
  });

  it("colours the river differently when the player gave up", () => {
    renderMap({ gameOver: true, gaveUp: true });

    expect(drawnLayers()[0].color).toBe(answerPathOptions(true).color);
  });

  it("locks panning while playing", () => {
    renderMap();
    expect(fakeMap.dragging.disable).toHaveBeenCalled();
  });
});
