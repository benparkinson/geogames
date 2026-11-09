import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { answerPathOptions, clueLineOptions, SATELLITE_ATTRIBUTION, SATELLITE_TILE_URL } from "../../map/constants";
import type { MapProps } from "../../map/MapProps";
import { drawnLayers, drawnTileAttributions, drawnTileUrls } from "../../test/leafletMock";
import { makeCountryShapeRoundData } from "../../test/fixtures";
import CoastlineMap from "./CoastlineMap";
import type { CountryShapeRound } from "./model";

vi.mock("react-leaflet", async () => await import("../../test/leafletMock"));

// In the fixture the layers start at: outline 1,1; coast 2,2; land borders 3,3; neighbours 4,4 and 5,5.
const OUTLINE = "1,1";
const COAST = "2,2";
const BORDERS = "3,3";

function renderMap(overrides: Partial<MapProps<CountryShapeRound>> = {}) {
  return render(<CoastlineMap data={makeCountryShapeRoundData()} gaveUp={false} gameOver={false} hintsUsed={0} {...overrides} />);
}

const points = () => drawnLayers().map((layer) => layer.point);

beforeEach(() => vi.clearAllMocks());

describe("CoastlineMap", () => {
  it("draws only the coastline on a blank map while playing", () => {
    renderMap();

    expect(drawnLayers()).toHaveLength(1);
    expect(drawnLayers()[0]).toMatchObject({ point: COAST, color: clueLineOptions().color, dashed: false });
    expect(drawnTileUrls()).toEqual([]);
  });

  it("does not draw the country's outline or its neighbours while playing, whatever the hints", () => {
    renderMap({ hintsUsed: 3 });

    expect(points()).not.toContain(OUTLINE);
    expect(points()).not.toContain("4,4");
    expect(points()).not.toContain("5,5");
  });

  it("adds the land borders as a dashed hint line only at the second hint", () => {
    const first = renderMap({ hintsUsed: 1 });
    expect(points()).not.toContain(BORDERS);
    first.unmount();

    renderMap({ hintsUsed: 2 });

    const borders = drawnLayers().find((layer) => layer.point === BORDERS);
    expect(borders?.dashed).toBe(true);
  });

  it("swaps the clue lines for the country's outline and satellite imagery once the round is over", () => {
    renderMap({ gameOver: true, hintsUsed: 2 });

    expect(drawnLayers()).toHaveLength(1);
    expect(drawnLayers()[0]).toMatchObject({ point: OUTLINE, color: answerPathOptions(false).color });
    expect(drawnTileUrls()).toEqual([SATELLITE_TILE_URL]);
    expect(drawnTileAttributions()).toEqual([SATELLITE_ATTRIBUTION]);
  });

  it("colours the outline differently when the player gave up", () => {
    renderMap({ gameOver: true, gaveUp: true });

    expect(drawnLayers()[0].color).toBe(answerPathOptions(true).color);
    expect(answerPathOptions(true).color).not.toBe(answerPathOptions(false).color);
  });
});
