import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { answerPathOptions, capitalDotOptions, CITY_MAX_RADIUS, cityDotOptions, RELIEF_TILE_URL } from "../../map/constants";
import type { MapProps } from "../../map/MapProps";
import { drawnCircles, drawnLayers, drawnTileUrls, fakeMap } from "../../test/leafletMock";
import { makeCountryCitiesRoundData, makeCountryShapeData } from "../../test/fixtures";
import CountryCitiesMap from "./CountryCitiesMap";
import type { CountryCitiesRound } from "./model";

vi.mock("react-leaflet", async () => await import("../../test/leafletMock"));

// In the fixture the country's layers start at: outline 1,1; coast 2,2; land borders 3,3.
const OUTLINE = "1,1";
const COAST = "2,2";

function renderMap(overrides: Partial<MapProps<CountryCitiesRound>> = {}, data = makeCountryCitiesRoundData()) {
  render(<CountryCitiesMap data={data} gaveUp={false} gameOver={false} hintsUsed={0} {...overrides} />);
}

const points = () => drawnLayers().map((layer) => layer.point);

beforeEach(() => vi.clearAllMocks());

describe("CountryCitiesMap", () => {
  it("draws only the cities, as dots, on a blank map while playing", () => {
    renderMap();

    expect(drawnLayers()).toEqual([]);
    expect(drawnTileUrls()).toEqual([]);
    expect(drawnCircles().map((circle) => circle.center)).toEqual([
      [52.37, 4.9],
      [51.92, 4.48],
      [52.09, 5.12],
    ]);
    expect(drawnCircles().every((circle) => circle.fill === cityDotOptions().fillColor)).toBe(true);
  });

  it("sizes the dots by population, the largest city getting the largest dot", () => {
    renderMap();

    const [amsterdam, rotterdam, utrecht] = drawnCircles().map((circle) => circle.radius);
    expect(amsterdam).toBe(CITY_MAX_RADIUS);
    expect(rotterdam).toBeLessThan(amsterdam);
    expect(utrecht).toBeLessThan(rotterdam);
  });

  it("shows no names while playing", () => {
    renderMap({ hintsUsed: 3 });

    expect(drawnCircles().every((circle) => circle.tooltip === "")).toBe(true);
  });

  it("fits the map to the country", () => {
    renderMap();

    expect(fakeMap.fitBounds).toHaveBeenCalledTimes(1);
  });

  it("fits the map to the country and to a city outside it", () => {
    const data = makeCountryCitiesRoundData();
    const { south, west, north, east } = data.country.focus;
    data.cities.push({ name: "Faraway", lon: east + 50, lat: south - 30, population: 1000, capital: false });
    renderMap({}, data);

    expect(fakeMap.fitBounds).toHaveBeenCalledWith(
      [
        [south - 30, west],
        [north, east + 50],
      ],
      expect.anything(),
    );
  });

  it("does not label the outline with the country's name once the round is over", () => {
    renderMap({ gameOver: true });

    expect(document.querySelector('[data-testid="GeoJSON"] [data-testid="Tooltip"]')).toBeNull();
  });

  it("draws the capital again in a different style at the first hint", () => {
    renderMap({ hintsUsed: 1 });

    const circles = drawnCircles();
    expect(circles).toHaveLength(4);
    expect(circles[3].center).toEqual([52.37, 4.9]);
    expect(circles[3].fill).toBe(capitalDotOptions().fillColor);
  });

  it("gives a capital with a tiny population a dot big enough to see", () => {
    const data = makeCountryCitiesRoundData({
      cities: [
        { name: "Big", lon: 1, lat: 1, population: 1_000_000, capital: false },
        { name: "Small capital", lon: 2, lat: 2, population: 10, capital: true },
      ],
    });
    renderMap({ hintsUsed: 1 }, data);

    expect(drawnCircles()[2].radius).toBeGreaterThanOrEqual(6);
  });

  it("adds the coastline as a dashed hint line at the second hint", () => {
    renderMap({ hintsUsed: 2 });

    expect(points()).toEqual([COAST]);
    expect(drawnLayers()[0].dashed).toBe(true);
  });

  it("draws no coastline for a country without one", () => {
    const landlocked = makeCountryCitiesRoundData({
      country: makeCountryShapeData({
        coastKm: 0,
        coast: { type: "MultiLineString", properties: {}, geometry: { type: "MultiLineString", coordinates: [] } },
      }),
    });
    renderMap({ hintsUsed: 2 }, landlocked);

    expect(drawnLayers()).toEqual([]);
  });

  it("shows the outline, the capital, the relief map and the city names once the round is over", () => {
    renderMap({ gameOver: true });

    expect(points()).toEqual([OUTLINE]);
    expect(drawnLayers()[0].color).toBe(answerPathOptions(false).color);
    expect(drawnTileUrls()).toEqual([RELIEF_TILE_URL]);
    expect(drawnCircles().map((circle) => circle.tooltip)).toEqual(["Amsterdam", "Rotterdam", "Utrecht", "Amsterdam"]);
  });

  it("colours the outline differently when the player gave up", () => {
    renderMap({ gameOver: true, gaveUp: true });

    expect(drawnLayers()[0].color).toBe(answerPathOptions(true).color);
  });

  it("draws no coastline once the round is over", () => {
    renderMap({ gameOver: true, hintsUsed: 3 });

    expect(points()).toEqual([OUTLINE]);
  });
});
