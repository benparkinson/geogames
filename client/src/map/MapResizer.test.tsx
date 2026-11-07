import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MapResizer } from "./MapResizer";

const container = document.createElement("div");
const fakeMap = vi.hoisted(() => ({
  invalidateSize: vi.fn(),
  getContainer: vi.fn(),
}));
vi.mock("react-leaflet", () => ({ useMap: () => fakeMap }));

class RecordingObserver {
  static instances: RecordingObserver[] = [];
  observe = vi.fn();
  disconnect = vi.fn();
  constructor(public callback: () => void) {
    RecordingObserver.instances.push(this);
  }
}

beforeEach(() => {
  RecordingObserver.instances = [];
  fakeMap.invalidateSize.mockClear();
  fakeMap.getContainer.mockReturnValue(container);
  vi.stubGlobal("ResizeObserver", RecordingObserver);
});

describe("MapResizer", () => {
  it("observes the map container", () => {
    render(<MapResizer />);

    expect(RecordingObserver.instances).toHaveLength(1);
    expect(RecordingObserver.instances[0].observe).toHaveBeenCalledWith(container);
  });

  it("tells the map to recalculate its size when the container resizes", () => {
    render(<MapResizer />);

    RecordingObserver.instances[0].callback();

    expect(fakeMap.invalidateSize).toHaveBeenCalledTimes(1);
  });

  it("stops observing when unmounted", () => {
    const { unmount } = render(<MapResizer />);

    unmount();

    expect(RecordingObserver.instances[0].disconnect).toHaveBeenCalledTimes(1);
  });
});
