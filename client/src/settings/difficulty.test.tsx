import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { resetDifficultyForTests, useDifficulty } from "./difficulty";

beforeEach(resetDifficultyForTests);

afterEach(() => {
  vi.restoreAllMocks();
});

describe("useDifficulty", () => {
  it("is Normal by default", () => {
    const { result } = renderHook(() => useDifficulty());

    expect(result.current[0]).toBe("NORMAL");
  });

  it("remembers Easy and Hard in localStorage and removes the key for Normal", () => {
    const { result } = renderHook(() => useDifficulty());

    act(() => result.current[1]("EASY"));
    expect(result.current[0]).toBe("EASY");
    expect(localStorage.getItem("gg-difficulty")).toBe("easy");
    expect(renderHook(() => useDifficulty()).result.current[0]).toBe("EASY");

    act(() => result.current[1]("HARD"));
    expect(result.current[0]).toBe("HARD");
    expect(localStorage.getItem("gg-difficulty")).toBe("hard");

    act(() => result.current[1]("NORMAL"));
    expect(result.current[0]).toBe("NORMAL");
    expect(localStorage.getItem("gg-difficulty")).toBeNull();
  });

  it("updates every hook instance in this tab", () => {
    const first = renderHook(() => useDifficulty());
    const second = renderHook(() => useDifficulty());

    act(() => first.result.current[1]("EASY"));

    expect(second.result.current[0]).toBe("EASY");
  });

  it("follows a change made in another tab", () => {
    const { result } = renderHook(() => useDifficulty());

    act(() => {
      localStorage.setItem("gg-difficulty", "hard");
      window.dispatchEvent(new StorageEvent("storage", { key: "gg-difficulty", newValue: "hard" }));
    });

    expect(result.current[0]).toBe("HARD");
  });

  it("ignores a value it doesn't know", () => {
    localStorage.setItem("gg-difficulty", "extreme");

    expect(renderHook(() => useDifficulty()).result.current[0]).toBe("NORMAL");
  });

  describe("the old Hard mode preference", () => {
    beforeEach(() => localStorage.setItem("gg-hardcore", "on"));

    it("counts as Hard while there is no difficulty, without rewriting storage on read", () => {
      const { result } = renderHook(() => useDifficulty());

      expect(result.current[0]).toBe("HARD");
      expect(localStorage.getItem("gg-hardcore")).toBe("on");
      expect(localStorage.getItem("gg-difficulty")).toBeNull();
    });

    it("is replaced by the first write of the new key", () => {
      const { result } = renderHook(() => useDifficulty());

      act(() => result.current[1]("EASY"));

      expect(result.current[0]).toBe("EASY");
      expect(localStorage.getItem("gg-hardcore")).toBeNull();
      expect(localStorage.getItem("gg-difficulty")).toBe("easy");
    });

    it("does not come back as Hard when the player then chooses Normal", () => {
      const { result } = renderHook(() => useDifficulty());

      act(() => result.current[1]("NORMAL"));

      expect(result.current[0]).toBe("NORMAL");
      expect(localStorage.getItem("gg-hardcore")).toBeNull();
    });

    it("loses to a difficulty that is stored", () => {
      localStorage.setItem("gg-difficulty", "easy");

      expect(renderHook(() => useDifficulty()).result.current[0]).toBe("EASY");
    });
  });

  it("still works for the page when localStorage is blocked", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    vi.spyOn(Storage.prototype, "removeItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    const { result } = renderHook(() => useDifficulty());

    act(() => result.current[1]("EASY"));
    expect(result.current[0]).toBe("EASY");

    act(() => result.current[1]("NORMAL"));
    expect(result.current[0]).toBe("NORMAL");
  });

  it("still takes effect for the page when saving fails but reading works", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("quota");
    });
    const { result } = renderHook(() => useDifficulty());

    act(() => result.current[1]("HARD"));

    expect(result.current[0]).toBe("HARD");
    act(() => result.current[1]("NORMAL"));
    expect(result.current[0]).toBe("NORMAL");
  });
});
