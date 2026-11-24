import { act, renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import { resetDistanceUnitForTests, useDistanceUnit } from "./units";

beforeEach(() => {
  resetDistanceUnitForTests();
  localStorage.clear();
});

describe("useDistanceUnit", () => {
  it("is km when nothing is stored, and for an unknown value", () => {
    expect(renderHook(() => useDistanceUnit()).result.current[0]).toBe("km");
    localStorage.setItem("gg-units", "furlongs");
    expect(renderHook(() => useDistanceUnit()).result.current[0]).toBe("km");
  });

  it("stores mi, and removes the key for km", () => {
    const { result } = renderHook(() => useDistanceUnit());
    act(() => result.current[1]("mi"));
    expect(result.current[0]).toBe("mi");
    expect(localStorage.getItem("gg-units")).toBe("mi");

    act(() => result.current[1]("km"));
    expect(result.current[0]).toBe("km");
    expect(localStorage.getItem("gg-units")).toBeNull();
  });

  it("is seen by a second hook instance", () => {
    const first = renderHook(() => useDistanceUnit());
    const second = renderHook(() => useDistanceUnit());
    act(() => first.result.current[1]("mi"));
    expect(second.result.current[0]).toBe("mi");
  });
});
