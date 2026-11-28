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

    await user.click(screen.getByRole("button", { name: /^Give up$/ }));
    expect(props.onGiveUp).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: /Tap again to give up/ })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Tap again to give up/ }));
    expect(props.onGiveUp).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: /^Give up$/ })).toBeInTheDocument();
  });

  it("keeps both labels in the button, so asking for the second tap doesn't change its width", async () => {
    // The longer "Tap again" label used to widen the button, wrap it onto a new line of the actions row and push it out
    // of the panel's scrolled view on a narrow phone: the button seemed to vanish on the first tap.
    const user = userEvent.setup();
    renderActions();
    const reveal = screen.getByRole("button", { name: /^Give up$/ });
    const labels = () => Array.from(reveal.querySelectorAll(".round-actions__reveal-label > span"));

    expect(labels().map((label) => label.textContent)).toEqual(["Give up", "Tap again to give up"]);
    expect(labels().map((label) => label.getAttribute("aria-hidden"))).toEqual([null, "true"]);

    await user.click(reveal);

    expect(screen.getByRole("button", { name: /Tap again to give up/ })).toBe(reveal);
    expect(labels().map((label) => label.getAttribute("aria-hidden"))).toEqual(["true", null]);
  });

  it("goes back to the first state after three seconds", () => {
    // Testing Library only detects Jest's fake timers, so user-event would hang here: use fireEvent.
    vi.useFakeTimers();
    const { props } = renderActions();

    fireEvent.click(screen.getByRole("button", { name: /^Give up$/ }));
    act(() => {
      vi.advanceTimersByTime(3000);
    });

    expect(screen.getByRole("button", { name: /^Give up$/ })).toBeInTheDocument();
    expect(props.onGiveUp).not.toHaveBeenCalled();
  });

  it("cleans up its timer when unmounted while waiting", () => {
    vi.useFakeTimers();
    const errors = vi.spyOn(console, "error").mockImplementation(() => {});
    const { unmount } = renderActions();

    fireEvent.click(screen.getByRole("button", { name: /^Give up$/ }));
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

    await userEvent.setup().click(screen.getByRole("button", { name: /Hint .*· 2 left/ }));

    expect(props.onHint).toHaveBeenCalledTimes(1);
  });

  it("gives the cost a unit, so it can't be read as a count of hints", () => {
    renderActions({ hintsUsed: 0, hintCount: 3 });

    expect(screen.getByRole("button", { name: "Hint −1 pt · 3 left" })).toBeInTheDocument();
  });

  it("disables the hint button when none are left", () => {
    renderActions({ hintsUsed: 2, hintCount: 2 });

    expect(screen.getByRole("button", { name: /Hint · 0 left/ })).toBeDisabled();
  });

  it("puts the cost of the next hint in the button's title", () => {
    const { rerender, props } = renderActions({ hintsUsed: 0, hintCount: 3 });
    // Every hint costs one point.
    expect(screen.getByTitle("Shows a clue (−1 point)")).toBeInTheDocument();

    rerender(<RoundActions {...props} hintsUsed={1} />);
    expect(screen.getByTitle("Shows a clue (−1 point)")).toBeInTheDocument();
    rerender(<RoundActions {...props} hintsUsed={2} />);
    expect(screen.getByTitle("Shows a clue (−1 point)")).toBeInTheDocument();
    rerender(<RoundActions {...props} hintsUsed={3} />);
    expect(screen.getByTitle("No hints left")).toBeInTheDocument();
  });

  it("says the next hint costs 0 points once the hint points are used up", () => {
    // Only the first three hints cost a point; a game with more hints than that gives the rest free.
    renderActions({ hintsUsed: 3, hintCount: 5 });
    expect(screen.getByTitle("Shows a clue (−0 points)")).toBeInTheDocument();
  });
});
