import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import ThemeToggle from "./ThemeToggle";

describe("ThemeToggle", () => {
  beforeEach(() => {
    document.documentElement.setAttribute("data-bs-theme", "light");
    localStorage.clear();
  });
  afterEach(() => {
    document.documentElement.removeAttribute("data-bs-theme");
  });

  it("switches to dark and remembers it", async () => {
    render(<ThemeToggle />);
    fireEvent.click(screen.getByRole("button", { name: "Switch to dark mode" }));
    expect(document.documentElement.getAttribute("data-bs-theme")).toBe("dark");
    expect(localStorage.getItem("gg-theme")).toBe("dark");
    expect(await screen.findByRole("button", { name: "Switch to light mode" })).toBeTruthy();
  });

  it("switches back to light", () => {
    document.documentElement.setAttribute("data-bs-theme", "dark");
    render(<ThemeToggle />);
    fireEvent.click(screen.getByRole("button", { name: "Switch to light mode" }));
    expect(document.documentElement.getAttribute("data-bs-theme")).toBe("light");
    expect(localStorage.getItem("gg-theme")).toBe("light");
  });
});
