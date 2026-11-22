import { act, fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { HintConfig } from "./hints";
import RoundActions from "./RoundActions";

const hints: HintConfig = { description: "Shows a clue" };

function renderActions(overrides: Partial<React.ComponentProps<typeof RoundActions>> = {}) {
  const props: React.ComponentProps<typeof RoundActions> = {
    hints,
    hintCount: 2,
    hintsUsed: 0,
    onHint: vi.fn(),
    onGiveUp: vi.fn(),
    ...overrides,
  };
  const view = render(<RoundActions {...props} />);
  return { ...view, props };
}

afterEach(() => {
  vi.useRealTimers();
});

describe("RoundActions reveal", () => {
  it("needs two clicks to reveal the answer", async () => {
    const user = userEvent.setup();
    const { props } = renderActions();

    await user.click(screen.getByRole("button", { name: /Give up this round/ }));
    expect(props.onGiveUp).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: /Tap again to give up/ })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Tap again to give up/ }));
    expect(props.onGiveUp).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: /Give up this round/ })).toBeInTheDocument();
  });

  it("goes back to the first state after three seconds", () => {
    // Testing Library only detects Jest's fake timers, so user-event would hang here: use fireEvent.
    vi.useFakeTimers();
    const { props } = renderActions();

    fireEvent.click(screen.getByRole("button", { name: /Give up this round/ }));
    act(() => {
      vi.advanceTimersByTime(3000);
    });

    expect(screen.getByRole("button", { name: /Give up this round/ })).toBeInTheDocument();
    expect(props.onGiveUp).not.toHaveBeenCalled();
  });

  it("cleans up its timer when unmounted while waiting", () => {
    vi.useFakeTimers();
    const errors = vi.spyOn(console, "error").mockImplementation(() => {});
    const { unmount } = renderActions();

    fireEvent.click(screen.getByRole("button", { name: /Give up this round/ }));
    unmount();

    expect(() => vi.advanceTimersByTime(5000)).not.toThrow();
    expect(vi.getTimerCount()).toBe(0);
    expect(errors).not.toHaveBeenCalled();
    errors.mockRestore();
  });
});

describe("RoundActions hints", () => {
  it("shows how many hints are left and uses one on click", async () => {
    const { props } = renderActions({ hintsUsed: 0, hintCount: 2 });

    await userEvent.setup().click(screen.getByRole("button", { name: /Hint \(.*, 2 left\)/ }));

    expect(props.onHint).toHaveBeenCalledTimes(1);
  });

  it("disables the hint button when none are left", () => {
    renderActions({ hintsUsed: 2, hintCount: 2 });

    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });

  it("puts the cost of the next hint in the button's title", () => {
    const { rerender, props } = renderActions({ hintsUsed: 0, hintCount: 3 });
    // 100 -> 83 -> 67 -> 50: the drops are 17, 16 and 17, not 50 / 3.
    expect(screen.getByTitle("Shows a clue (−17 pts)")).toBeInTheDocument();

    rerender(<RoundActions {...props} hintsUsed={1} />);
    expect(screen.getByTitle("Shows a clue (−16 pts)")).toBeInTheDocument();
    rerender(<RoundActions {...props} hintsUsed={2} />);
    expect(screen.getByTitle("Shows a clue (−17 pts)")).toBeInTheDocument();
    rerender(<RoundActions {...props} hintsUsed={3} />);
    expect(screen.getByTitle("No hints left")).toBeInTheDocument();
  });
});
