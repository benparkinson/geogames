import { act, renderHook } from "@testing-library/react";
import { renderToString } from "react-dom/server";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { resetThemeForTests, useThemeChoice } from "./theme";

function mockOsDark(dark: boolean): void {
  vi.stubGlobal("matchMedia", (query: string) => ({ matches: dark, media: query }));
}

beforeEach(() => {
  resetThemeForTests();
  localStorage.clear();
  document.documentElement.removeAttribute("data-theme");
  mockOsDark(false);
});

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe("useThemeChoice", () => {
  it("is system when nothing is stored, and for an unknown value", () => {
    expect(renderHook(() => useThemeChoice()).result.current[0]).toBe("system");
    localStorage.setItem("gg-theme", "purple");
    expect(renderHook(() => useThemeChoice()).result.current[0]).toBe("system");
  });

  it("reads an old saved light or dark", () => {
    localStorage.setItem("gg-theme", "dark");
    expect(renderHook(() => useThemeChoice()).result.current[0]).toBe("dark");
  });

  it.each(["dark", "light"] as const)("stores %s and sets data-theme", (choice) => {
    const { result } = renderHook(() => useThemeChoice());
    act(() => result.current[1](choice));
    expect(result.current[0]).toBe(choice);
    expect(document.documentElement.getAttribute("data-theme")).toBe(choice);
    expect(localStorage.getItem("gg-theme")).toBe(choice);
  });

  it.each([
    [true, "dark"],
    [false, "light"],
  ])("system removes the key and follows an OS that is dark=%s", (dark, expected) => {
    mockOsDark(dark);
    localStorage.setItem("gg-theme", "dark");
    const { result } = renderHook(() => useThemeChoice());
    act(() => result.current[1]("system"));
    expect(result.current[0]).toBe("system");
    expect(localStorage.getItem("gg-theme")).toBeNull();
    expect(document.documentElement.getAttribute("data-theme")).toBe(expected);
  });

  it("still switches when storage is blocked, and remembers the choice in memory", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    const { result } = renderHook(() => useThemeChoice());
    act(() => result.current[1]("dark"));
    expect(document.documentElement.getAttribute("data-theme")).toBe("dark");
    expect(result.current[0]).toBe("dark");
  });

  it("follows a storage event from another tab", () => {
    const { result } = renderHook(() => useThemeChoice());
    act(() => {
      localStorage.setItem("gg-theme", "light");
      window.dispatchEvent(new Event("storage"));
    });
    expect(result.current[0]).toBe("light");
  });

  it("is system on the server", () => {
    function Probe(): React.JSX.Element {
      return <span>{useThemeChoice()[0]}</span>;
    }
    expect(renderToString(<Probe />)).toContain("system");
  });
});
