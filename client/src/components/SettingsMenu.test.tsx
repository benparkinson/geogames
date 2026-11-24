import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderToString } from "react-dom/server";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { resetThemeForTests } from "../settings/theme";
import { resetDistanceUnitForTests } from "../settings/units";
import SiteFooter from "./SiteFooter";
import SettingsMenu from "./SettingsMenu";

beforeEach(() => {
  resetThemeForTests();
  resetDistanceUnitForTests();
  localStorage.clear();
  document.documentElement.removeAttribute("data-theme");
  vi.stubGlobal("matchMedia", (query: string) => ({ matches: false, media: query }));
});
afterEach(() => vi.unstubAllGlobals());

describe("SettingsMenu", () => {
  it("is hidden until Settings is clicked", async () => {
    render(<SettingsMenu />);
    const button = screen.getByRole("button", { name: "Settings" });
    expect(button.getAttribute("aria-expanded")).toBe("false");
    expect(screen.queryByRole("radio")).toBeNull();

    await userEvent.click(button);
    expect(button.getAttribute("aria-expanded")).toBe("true");
    expect(screen.getByRole("radio", { name: "System default" })).toBeChecked();
  });

  it("shows the stored choice", async () => {
    localStorage.setItem("gg-theme", "dark");
    render(<SettingsMenu />);
    await userEvent.click(screen.getByRole("button", { name: "Settings" }));
    expect(screen.getByRole("radio", { name: "Dark" })).toBeChecked();
  });

  it("changes the theme and stays open", async () => {
    render(<SettingsMenu />);
    await userEvent.click(screen.getByRole("button", { name: "Settings" }));

    await userEvent.click(screen.getByRole("radio", { name: "Dark" }));
    expect(document.documentElement.getAttribute("data-theme")).toBe("dark");
    expect(localStorage.getItem("gg-theme")).toBe("dark");

    await userEvent.click(screen.getByRole("radio", { name: "System default" }));
    expect(localStorage.getItem("gg-theme")).toBeNull();
    expect(document.documentElement.getAttribute("data-theme")).toBe("light");
    expect(screen.getByRole("radio", { name: "System default" })).toBeChecked();
  });

  it("shows the Distances control, defaulting to Kilometres", async () => {
    render(<SettingsMenu />);
    await userEvent.click(screen.getByRole("button", { name: "Settings" }));
    expect(screen.getByRole("radio", { name: "Kilometres" })).toBeChecked();
  });

  it("stores Miles and stays open", async () => {
    render(<SettingsMenu />);
    const button = screen.getByRole("button", { name: "Settings" });
    await userEvent.click(button);
    await userEvent.click(screen.getByRole("radio", { name: "Miles" }));
    expect(localStorage.getItem("gg-units")).toBe("mi");
    expect(button.getAttribute("aria-expanded")).toBe("true");
    expect(screen.getByRole("radio", { name: "Miles" })).toBeChecked();
  });

  it("shows a stored Miles choice", async () => {
    localStorage.setItem("gg-units", "mi");
    render(<SettingsMenu />);
    await userEvent.click(screen.getByRole("button", { name: "Settings" }));
    expect(screen.getByRole("radio", { name: "Miles" })).toBeChecked();
  });

  it("closes on Escape and returns focus to the button", async () => {
    render(<SettingsMenu />);
    const button = screen.getByRole("button", { name: "Settings" });
    await userEvent.click(button);
    await userEvent.keyboard("{Escape}");
    expect(button.getAttribute("aria-expanded")).toBe("false");
    expect(button).toHaveFocus();
  });

  it("closes on a pointerdown outside, and on pressing the button again", async () => {
    render(<SettingsMenu />);
    const button = screen.getByRole("button", { name: "Settings" });
    await userEvent.click(button);
    await userEvent.click(document.body);
    expect(button.getAttribute("aria-expanded")).toBe("false");

    await userEvent.click(button);
    await userEvent.click(button);
    expect(button.getAttribute("aria-expanded")).toBe("false");
  });

  it("renders closed on the server", () => {
    const html = renderToString(<SiteFooter />);
    expect(html).toContain("Settings");
    expect(html).not.toContain("Appearance");
  });
});
