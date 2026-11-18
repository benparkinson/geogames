import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { resetHardModeForTests, useHardMode } from "./hardMode";

beforeEach(resetHardModeForTests);

afterEach(() => {
  vi.restoreAllMocks();
});

describe("useHardMode", () => {
  it("is off by default", () => {
    const { result } = renderHook(() => useHardMode());

    expect(result.current[0]).toBe(false);
  });

  it("remembers the setting in localStorage and removes the key when turned off", () => {
    const { result } = renderHook(() => useHardMode());

    act(() => result.current[1](true));
    expect(result.current[0]).toBe(true);
    expect(localStorage.getItem("gg-hardcore")).toBe("on");
    expect(renderHook(() => useHardMode()).result.current[0]).toBe(true);

    act(() => result.current[1](false));
    expect(result.current[0]).toBe(false);
    expect(localStorage.getItem("gg-hardcore")).toBeNull();
  });

  it("updates every hook instance in this tab", () => {
    const first = renderHook(() => useHardMode());
    const second = renderHook(() => useHardMode());

    act(() => first.result.current[1](true));

    expect(second.result.current[0]).toBe(true);
  });

  it("follows a change made in another tab", () => {
    const { result } = renderHook(() => useHardMode());

    act(() => {
      localStorage.setItem("gg-hardcore", "on");
      window.dispatchEvent(new StorageEvent("storage", { key: "gg-hardcore", newValue: "on" }));
    });

    expect(result.current[0]).toBe(true);
  });

  it("still works for the page when localStorage is blocked", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    const { result } = renderHook(() => useHardMode());

    act(() => result.current[1](true));
    expect(result.current[0]).toBe(true);

    act(() => result.current[1](false));
    expect(result.current[0]).toBe(false);
  });

  it("still takes effect for the page when saving fails but reading works", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("quota");
    });
    const { result } = renderHook(() => useHardMode());

    act(() => result.current[1](true));

    expect(result.current[0]).toBe(true);
    act(() => result.current[1](false));
    expect(result.current[0]).toBe(false);
  });
});
