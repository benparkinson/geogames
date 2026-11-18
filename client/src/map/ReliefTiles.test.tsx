import { render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { drawnTileAttributions, drawnTileUrls } from "../test/leafletMock";
import { RELIEF_MAX_NATIVE_ZOOM, RELIEF_TILE_URL } from "./constants";
import { ReliefTiles } from "./ReliefTiles";

vi.mock("react-leaflet", async () => await import("../test/leafletMock"));

describe("ReliefTiles", () => {
  it("draws the self-hosted tiles, stretched past their last zoom, with no attribution of their own", () => {
    const { container } = render(<ReliefTiles />);

    expect(drawnTileUrls()).toEqual([RELIEF_TILE_URL]);
    expect(RELIEF_TILE_URL).toBe("/tiles/relief/{z}/{x}/{y}.jpg");
    expect(container.querySelector("[data-max-native-zoom]")?.getAttribute("data-max-native-zoom")).toBe(
      String(RELIEF_MAX_NATIVE_ZOOM),
    );
    expect(RELIEF_MAX_NATIVE_ZOOM).toBe(6);
    expect(drawnTileAttributions()).toEqual([""]);
  });
});
