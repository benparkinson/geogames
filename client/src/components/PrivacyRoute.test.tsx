import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import PrivacyRoute from "../../pages/privacy";

// The test lives here, not under pages/, because Next would treat a test file there as a route.
describe("the /privacy route", () => {
  it("renders the privacy page", () => {
    render(<PrivacyRoute />);

    expect(screen.getByRole("heading", { name: "Credits and privacy", level: 1 })).toBeInTheDocument();
  });
});
