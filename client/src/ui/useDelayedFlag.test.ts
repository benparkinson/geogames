import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useDelayedFlag } from "./useDelayedFlag";

beforeEach(() => vi.useFakeTimers());
afterEach(() => vi.useRealTimers());

describe("useDelayedFlag", () => {
  it("is false at first, and true once it has been active for the delay", () => {
    const { result } = renderHook(() => useDelayedFlag(true, 3000));
    expect(result.current).toBe(false);

    act(() => {
      vi.advanceTimersByTime(2999);
    });
    expect(result.current).toBe(false);

    act(() => {
      vi.advanceTimersByTime(1);
    });
    expect(result.current).toBe(true);
  });

  it("never turns true while it is not active", () => {
    const { result } = renderHook(() => useDelayedFlag(false, 3000));

    act(() => {
      vi.advanceTimersByTime(10000);
    });

    expect(result.current).toBe(false);
  });

  it("goes back to false when it stops being active, and starts counting again next time", () => {
    const { result, rerender } = renderHook(({ active }) => useDelayedFlag(active, 3000), {
      initialProps: { active: true },
    });
    act(() => {
      vi.advanceTimersByTime(3000);
    });
    expect(result.current).toBe(true);

    rerender({ active: false });
    expect(result.current).toBe(false);

    rerender({ active: true });
    act(() => {
      vi.advanceTimersByTime(2000);
    });
    expect(result.current).toBe(false);
    act(() => {
      vi.advanceTimersByTime(1000);
    });
    expect(result.current).toBe(true);
  });

  it("clears its timer when unmounted", () => {
    const { unmount } = renderHook(() => useDelayedFlag(true, 3000));

    unmount();

    expect(vi.getTimerCount()).toBe(0);
  });
});
