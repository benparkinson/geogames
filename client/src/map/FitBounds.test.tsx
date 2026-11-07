import { render } from "@testing-library/react";
import type { GeoJsonObject } from "geojson";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { FitBounds } from "./FitBounds";

const fakeMap = vi.hoisted(() => ({ fitBounds: vi.fn() }));
vi.mock("react-leaflet", () => ({ useMap: () => fakeMap }));

const line = (end: number): GeoJsonObject =>
  ({
    type: "Feature",
    properties: {},
    geometry: { type: "LineString", coordinates: [[0, 0], [end, end]] },
  }) as GeoJsonObject;

beforeEach(() => {
  fakeMap.fitBounds.mockClear();
});

describe("FitBounds", () => {
  it("fits the map to the data with padding and a maximum zoom", () => {
    render(<FitBounds data={line(10)} id="danube" />);

    expect(fakeMap.fitBounds).toHaveBeenCalledTimes(1);
    const [bounds, options] = fakeMap.fitBounds.mock.calls[0];
    expect(bounds.getNorth()).toBe(10);
    expect(bounds.getEast()).toBe(10);
    expect(bounds.getSouth()).toBe(0);
    expect(options).toEqual({ padding: [24, 24], maxZoom: 7 });
  });

  it("fits again when the id changes", () => {
    const { rerender } = render(<FitBounds data={line(10)} id="danube" />);

    rerender(<FitBounds data={line(20)} id="rhine" />);

    expect(fakeMap.fitBounds).toHaveBeenCalledTimes(2);
    expect(fakeMap.fitBounds.mock.calls[1][0].getNorth()).toBe(20);
  });

  it("does not fit again when only the data object changes", () => {
    // Deliberate: the effect depends on `id` alone, so a new data object for the same river
    // does not move the map.
    const { rerender } = render(<FitBounds data={line(10)} id="danube" />);

    rerender(<FitBounds data={line(20)} id="danube" />);

    expect(fakeMap.fitBounds).toHaveBeenCalledTimes(1);
  });
});
