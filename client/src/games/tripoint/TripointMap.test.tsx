import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { answerPathOptions, SATELLITE_ATTRIBUTION, SATELLITE_TILE_URL } from "../../map/constants";
import type { MapProps } from "../../map/MapProps";
import { drawnLayers, drawnMarkers, drawnTileAttributions, drawnTileUrls, fakeMap } from "../../test/leafletMock";
import { makeTripointData } from "../../test/fixtures";
import TripointMap from "./TripointMap";
import type { TripointRoundData } from "./model";

vi.mock("react-leaflet", async () => await import("../../test/leafletMock"));

// In the fixture Belgium's outline starts at 0,0, the Netherlands' at 1,1 and Germany's at 2,2.
function roundData(): TripointRoundData {
  return { ...makeTripointData(), hintOrder: ["Germany", "Belgium", "Netherlands"] };
}

function renderMap(overrides: Partial<MapProps<TripointRoundData>> = {}) {
  return render(<TripointMap data={roundData()} gaveUp={false} gameOver={false} hintsUsed={0} {...overrides} />);
}

const points = () => drawnLayers().map((layer) => layer.point);

beforeEach(() => vi.clearAllMocks());

describe("TripointMap", () => {
  it("shows satellite imagery and a flag on the tripoint but no borders at first", () => {
    renderMap();

    expect(drawnTileUrls()).toEqual([SATELLITE_TILE_URL]);
    expect(drawnTileAttributions()).toEqual([SATELLITE_ATTRIBUTION]);
    expect(drawnMarkers()).toEqual([[50.75, 6.02]]);
    expect(drawnLayers()).toEqual([]);
  });

  it("centres the map on the tripoint", () => {
    renderMap();

    expect(fakeMap.setView).toHaveBeenCalledWith([50.75, 6.02], 5);
  });

  it("outlines the hinted countries in hint order, and no others, while playing", () => {
    const first = renderMap({ hintsUsed: 1 });
    expect(points()).toEqual(["2,2"]);
    first.unmount();

    renderMap({ hintsUsed: 2 });
    expect(points()).toEqual(["0,0", "2,2"]);
  });

  it("outlines all three countries, named, once the round is over", () => {
    renderMap({ gameOver: true });

    expect(points().sort()).toEqual(["0,0", "1,1", "2,2"]);
    expect(screen.getAllByTestId("Tooltip").map((tip) => tip.textContent)).toEqual(["Belgium", "Netherlands", "Germany"]);
    expect(drawnLayers().every((layer) => layer.color === answerPathOptions(false).color)).toBe(true);
  });

  it("does not repeat the hint outlines when the round is over", () => {
    renderMap({ gameOver: true, hintsUsed: 3 });

    expect(drawnLayers()).toHaveLength(3);
  });

  it("colours the borders differently when the player gave up", () => {
    renderMap({ gameOver: true, gaveUp: true });

    expect(drawnLayers().every((layer) => layer.color === answerPathOptions(true).color)).toBe(true);
    expect(answerPathOptions(true).color).not.toBe(answerPathOptions(false).color);
  });
});
