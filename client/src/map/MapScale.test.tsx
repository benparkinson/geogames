import { act, render, renderHook, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { resetDistanceUnitForTests, useDistanceUnit } from "../settings/units";
import { MapScale, SCALE_MAX_WIDTH } from "./MapScale";

vi.mock("react-leaflet", async () => await import("../test/leafletMock"));

beforeEach(() => {
  resetDistanceUnitForTests();
  localStorage.clear();
});

describe("MapScale", () => {
  it("shows a metric bar in the bottom left corner by default", () => {
    render(<MapScale />);

    const scale = screen.getByTestId("ScaleControl");
    expect(scale).toHaveAttribute("data-position", "bottomleft");
    expect(scale).toHaveAttribute("data-max-width", String(SCALE_MAX_WIDTH));
    expect(scale).toHaveAttribute("data-metric", "true");
    expect(scale).toHaveAttribute("data-imperial", "false");
  });

  it("shows miles when the player chose them", () => {
    localStorage.setItem("gg-units", "mi");
    render(<MapScale />);

    expect(screen.getByTestId("ScaleControl")).toHaveAttribute("data-metric", "false");
    expect(screen.getByTestId("ScaleControl")).toHaveAttribute("data-imperial", "true");
  });

  it("switches unit when the setting changes", () => {
    render(<MapScale />);
    const setting = renderHook(() => useDistanceUnit());

    act(() => setting.result.current[1]("mi"));

    expect(screen.getByTestId("ScaleControl")).toHaveAttribute("data-imperial", "true");
    expect(screen.getByTestId("ScaleControl")).toHaveAttribute("data-metric", "false");
  });
});
