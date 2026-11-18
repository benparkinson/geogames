import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MapCredits } from "./MapCredits";

const fakeMap = vi.hoisted(() => ({
  attributionControl: { addAttribution: vi.fn(), removeAttribution: vi.fn() },
}));
vi.mock("react-leaflet", () => ({ useMap: () => fakeMap }));

const CREDITS = 'Map data: Natural Earth | <a href="/privacy">Privacy</a>';

beforeEach(() => {
  fakeMap.attributionControl.addAttribution.mockClear();
  fakeMap.attributionControl.removeAttribution.mockClear();
});

describe("MapCredits", () => {
  it("adds the data credit and the privacy link to the attribution control", () => {
    render(<MapCredits />);

    expect(fakeMap.attributionControl.addAttribution).toHaveBeenCalledExactlyOnceWith(CREDITS);
  });

  it("puts an extra credit first", () => {
    render(<MapCredits extra="Tripoints: Wikipedia" />);

    expect(fakeMap.attributionControl.addAttribution).toHaveBeenCalledExactlyOnceWith(
      "Tripoints: Wikipedia | " + CREDITS,
    );
  });

  it("removes exactly what it added when it unmounts", () => {
    const { unmount } = render(<MapCredits extra="Tripoints: Wikipedia" />);
    const added = fakeMap.attributionControl.addAttribution.mock.calls[0][0];

    unmount();

    expect(fakeMap.attributionControl.removeAttribution).toHaveBeenCalledExactlyOnceWith(added);
  });

  it("renders nothing itself", () => {
    const { container } = render(<MapCredits />);

    expect(container).toBeEmptyDOMElement();
  });
});
