import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CenterUpdater } from "./CenterUpdater";

const fakeMap = vi.hoisted(() => ({ setView: vi.fn() }));
vi.mock("react-leaflet", () => ({ useMap: () => fakeMap }));

beforeEach(() => {
  fakeMap.setView.mockClear();
});

describe("CenterUpdater", () => {
  it("centres the map on mount with the default zoom", () => {
    render(<CenterUpdater center={[10, 20]} />);

    expect(fakeMap.setView).toHaveBeenCalledTimes(1);
    expect(fakeMap.setView).toHaveBeenCalledWith([10, 20], 4);
  });

  it("uses the zoom it is given", () => {
    render(<CenterUpdater center={[10, 20]} zoom={7} />);

    expect(fakeMap.setView).toHaveBeenCalledWith([10, 20], 7);
  });

  it("does not move the map again for a re-render with the same centre", () => {
    const { rerender } = render(<CenterUpdater center={[10, 20]} />);

    rerender(<CenterUpdater center={[10, 20]} />);

    expect(fakeMap.setView).toHaveBeenCalledTimes(1);
  });

  it("moves the map when the centre or zoom changes", () => {
    const { rerender } = render(<CenterUpdater center={[10, 20]} />);

    rerender(<CenterUpdater center={[11, 20]} />);
    rerender(<CenterUpdater center={[11, 20]} zoom={6} />);

    expect(fakeMap.setView).toHaveBeenCalledTimes(3);
    expect(fakeMap.setView).toHaveBeenLastCalledWith([11, 20], 6);
  });

  it("renders nothing", () => {
    const { container } = render(<CenterUpdater center={[0, 0]} />);

    expect(container).toBeEmptyDOMElement();
  });
});
