import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import SiteFooter from "./SiteFooter";

describe("SiteFooter", () => {
  it("is one link to the credits and privacy page", () => {
    render(<SiteFooter />);

    const footer = within(screen.getByRole("contentinfo"));
    expect(footer.getAllByRole("link")).toHaveLength(1);
    expect(footer.getByRole("link", { name: "Credits and privacy" }).getAttribute("href")).toBe("/privacy");
  });
});
