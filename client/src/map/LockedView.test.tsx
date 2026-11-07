import { render } from "@testing-library/react";
import L from "leaflet";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LockedView } from "./LockedView";

const fakeMap = vi.hoisted(() => ({
  getBoundsZoom: vi.fn(),
  setMinZoom: vi.fn(),
  setMaxZoom: vi.fn(),
  setView: vi.fn(),
  dragging: { enable: vi.fn(), disable: vi.fn() },
  keyboard: { enable: vi.fn(), disable: vi.fn() },
  boxZoom: { enable: vi.fn(), disable: vi.fn() },
}));
vi.mock("react-leaflet", () => ({ useMap: () => fakeMap }));

const bounds = L.latLngBounds([0, 0], [10, 20]);

beforeEach(() => {
  vi.clearAllMocks();
  fakeMap.getBoundsZoom.mockReturnValue(5);
});

describe("LockedView", () => {
  it("fits the bounds and limits the zoom around that", () => {
    render(<LockedView bounds={bounds} id="a" locked zoomOut={2} zoomIn={1} />);

    expect(fakeMap.getBoundsZoom).toHaveBeenCalledWith(bounds, false, L.point(48, 48));
    expect(fakeMap.setMinZoom).toHaveBeenLastCalledWith(3);
    expect(fakeMap.setMaxZoom).toHaveBeenLastCalledWith(6);
    expect(fakeMap.setView).toHaveBeenCalledWith(bounds.getCenter(), 5, { animate: false });
  });

  it("moves to the bounds before narrowing the zoom, so Leaflet can't zoom around the old centre", () => {
    fakeMap.getBoundsZoom.mockReturnValue(8);

    render(<LockedView bounds={bounds} id="a" locked zoomOut={1} zoomIn={1} />);

    const setViewOrder = fakeMap.setView.mock.invocationCallOrder[0];
    const narrowMin = fakeMap.setMinZoom.mock.calls.findIndex(([z]) => z === 7);
    const narrowMax = fakeMap.setMaxZoom.mock.calls.findIndex(([z]) => z === 9);
    expect(fakeMap.setMinZoom.mock.invocationCallOrder[narrowMin]).toBeGreaterThan(setViewOrder);
    expect(fakeMap.setMaxZoom.mock.invocationCallOrder[narrowMax]).toBeGreaterThan(setViewOrder);
    // Before the move, the limits only ever widen to include the target zoom.
    expect(fakeMap.setMinZoom.mock.calls[0][0]).toBeLessThanOrEqual(8);
    expect(fakeMap.setMaxZoom.mock.calls[0][0]).toBeGreaterThanOrEqual(8);
  });

  it("turns off panning while locked", () => {
    render(<LockedView bounds={bounds} id="a" locked />);

    expect(fakeMap.dragging.disable).toHaveBeenCalled();
    expect(fakeMap.keyboard.disable).toHaveBeenCalled();
    expect(fakeMap.boxZoom.disable).toHaveBeenCalled();
    expect(fakeMap.dragging.enable).not.toHaveBeenCalled();
  });

  it("unlocks panning and widens the zoom when the round ends", () => {
    const { rerender } = render(<LockedView bounds={bounds} id="a" locked />);

    rerender(<LockedView bounds={bounds} id="a" locked={false} />);

    expect(fakeMap.dragging.enable).toHaveBeenCalled();
    expect(fakeMap.keyboard.enable).toHaveBeenCalled();
    expect(fakeMap.boxZoom.enable).toHaveBeenCalled();
    expect(fakeMap.setMinZoom).toHaveBeenLastCalledWith(2);
    expect(fakeMap.setMaxZoom).toHaveBeenLastCalledWith(10);
    expect(fakeMap.setView).toHaveBeenCalledTimes(1);
  });

  it("fits again when the id changes", () => {
    const { rerender } = render(<LockedView bounds={bounds} id="a" locked />);

    rerender(<LockedView bounds={bounds} id="b" locked />);

    expect(fakeMap.setView).toHaveBeenCalledTimes(2);
  });
});
