import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { answerPathOptions, RELIEF_TILE_URL } from "../../map/constants";
import type { MapProps } from "../../map/MapProps";
import { drawnLayers, drawnTileUrls, fakeMap } from "../../test/leafletMock";
import { makeLakeRoundData } from "../../test/fixtures";
import LakeShapesMap from "./LakeShapesMap";
import type { LakeRound } from "./model";

vi.mock("react-leaflet", async () => await import("../../test/leafletMock"));

function renderMap(overrides: Partial<MapProps<LakeRound>> = {}) {
  render(<LakeShapesMap data={makeLakeRoundData()} gaveUp={false} gameOver={false} hintsUsed={0} {...overrides} />);
}

beforeEach(() => vi.clearAllMocks());

describe("LakeShapesMap", () => {
  it("draws the lake's shape on a blank map while playing", () => {
    renderMap();

    expect(drawnLayers()).toHaveLength(1);
    expect(drawnLayers()[0].color).toBe(answerPathOptions(false).color);
    expect(drawnTileUrls()).toEqual([]);
  });

  it("shows the relief map only once the round is over", () => {
    renderMap({ gameOver: true });

    expect(drawnTileUrls()).toEqual([RELIEF_TILE_URL]);
  });

  it("colours the lake differently when the player gave up or ran out of guesses", () => {
    renderMap({ gameOver: true, gaveUp: true });

    expect(drawnLayers()[0].color).toBe(answerPathOptions(true).color);
    expect(answerPathOptions(true).color).not.toBe(answerPathOptions(false).color);
  });

  it("fits the map to the lake, allowing a closer zoom than for rivers", () => {
    renderMap();

    expect(fakeMap.fitBounds).toHaveBeenCalledWith(expect.anything(), expect.objectContaining({ maxZoom: 11 }));
  });
});
