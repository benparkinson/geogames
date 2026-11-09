import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { renderToString } from "react-dom/server";
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

  it("still switches when storage is blocked", () => {
    const setItem = vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    render(<ThemeToggle />);
    fireEvent.click(screen.getByRole("button", { name: "Switch to dark mode" }));
    expect(document.documentElement.getAttribute("data-bs-theme")).toBe("dark");
    setItem.mockRestore();
  });

  it("stops watching the page theme when unmounted", () => {
    const { unmount } = render(<ThemeToggle />);
    unmount();
    document.documentElement.setAttribute("data-bs-theme", "dark");
    expect(screen.queryByRole("button")).toBeNull();
  });

  it("renders the light button on the server", () => {
    expect(renderToString(<ThemeToggle />)).toContain("Switch to dark mode");
  });

  it("switches back to light", () => {
    document.documentElement.setAttribute("data-bs-theme", "dark");
    render(<ThemeToggle />);
    fireEvent.click(screen.getByRole("button", { name: "Switch to light mode" }));
    expect(document.documentElement.getAttribute("data-bs-theme")).toBe("light");
    expect(localStorage.getItem("gg-theme")).toBe("light");
  });
});
