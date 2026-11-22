import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { resetClockForTests, useNowSeconds } from "./useNowSeconds";

function Clock({ id }: { id: string }) {
  const now = useNowSeconds();
  return <div data-testid={id}>{now}</div>;
}

beforeEach(() => {
  vi.useFakeTimers();
  vi.setSystemTime(new Date(2026, 5, 10, 12, 0, 0));
});

afterEach(() => {
  resetClockForTests();
  vi.useRealTimers();
});

describe("useNowSeconds", () => {
  it("updates once a second", () => {
    render(<Clock id="a" />);
    const first = Number(screen.getByTestId("a").textContent);

    act(() => {
      vi.advanceTimersByTime(3000);
    });

    expect(Number(screen.getByTestId("a").textContent)).toBe(first + 3);
  });

  it("shares one timer between components and stops it with the last one", () => {
    const a = render(<Clock id="a" />);
    const b = render(<Clock id="b" />);
    expect(vi.getTimerCount()).toBe(1);

    a.unmount();
    expect(vi.getTimerCount()).toBe(1);
    b.unmount();
    expect(vi.getTimerCount()).toBe(0);
  });
});
