import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { LANDING_TYPES, landingPath } from "../games/landing";
import SiteFooter from "./SiteFooter";

describe("SiteFooter", () => {
  it("links to the credits and privacy page", () => {
    render(<SiteFooter />);

    const footer = within(screen.getByRole("contentinfo"));
    expect(footer.getByRole("link", { name: "Credits and privacy" }).getAttribute("href")).toBe("/privacy");
  });

  it("lists every game's landing page", () => {
    render(<SiteFooter />);

    const links = [...screen.getByRole("contentinfo").querySelectorAll("details a")];
    expect(links.map((link) => link.getAttribute("href"))).toEqual(LANDING_TYPES.map(landingPath));
  });
});
