import { act, fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { makeRoundProps } from "../../test/fixtures";
import { HintConfig } from "./MapGame";
import { GuessResult } from "./guessReducer";
import MapGameInput from "./MapGameInput";

function renderInput(overrides: Partial<React.ComponentProps<typeof MapGameInput>> = {}) {
  const props: React.ComponentProps<typeof MapGameInput> = {
    gameOver: false,
    gaveUp: false,
    round: makeRoundProps(),
    wrongGuesses: [],
    submitGuess: vi.fn<(guess: string) => GuessResult>(() => ({ kind: "over" })),
    onHint: vi.fn(),
    hintsUsed: 0,
    hintCount: 0,
    giveUp: vi.fn(),
    inputPlaceholder: "Country…",
    answerNoun: "country",
    ...overrides,
  };
  const view = render(<MapGameInput {...props} />);
  return { ...view, props };
}

const input = () => screen.getByRole("textbox", { name: "Country…" });

afterEach(() => {
  vi.useRealTimers();
});

describe("MapGameInput feedback", () => {
  async function submitting(result: GuessResult, text = "Spain") {
    const user = userEvent.setup();
    const view = renderInput({ submitGuess: vi.fn(() => result) });
    await user.type(input(), text);
    await user.click(screen.getByRole("button", { name: "Guess" }));
    return view;
  }

  it("asks for a guess when it is empty", async () => {
    await submitting({ kind: "empty" });

    expect(screen.getByText("Type a guess first.")).toBeInTheDocument();
  });

  it("says when a guess was already made", async () => {
    await submitting({ kind: "duplicate", guess: "Spain" });

    expect(screen.getByText("You've already guessed Spain.")).toBeInTheDocument();
  });

  it("says when a guess is not a known country and keeps the text", async () => {
    await submitting({ kind: "unknown", guess: "Spaan" }, "Spaan");

    expect(screen.getByText('We don\'t recognise "Spaan" as a country. Check the spelling.')).toBeInTheDocument();
    expect(input()).toHaveValue("Spaan");
  });

  it("says when a guess is wrong and clears the text", async () => {
    await submitting({ kind: "wrong", guess: "Spain" });

    expect(screen.getByText('"Spain" isn\'t right. Try again.')).toBeInTheDocument();
    expect(input()).toHaveValue("");
  });

  it("counts the answers still to find after a correct guess", async () => {
    await submitting({ kind: "correct", remaining: 2 });

    expect(screen.getByText("Correct! 2 to go.")).toBeInTheDocument();
    expect(input()).toHaveValue("");
  });

  it("just says correct when nothing remains", async () => {
    await submitting({ kind: "correct", remaining: 0 });

    expect(screen.getByText("Correct!")).toBeInTheDocument();
  });

  it("shows no feedback once the round is over", async () => {
    const { container } = await submitting({ kind: "over" });

    expect(container.querySelector(".guess-feedback")).toBeEmptyDOMElement();
  });
});

describe("MapGameInput submitting", () => {
  it("submits the typed text on Enter", async () => {
    const { props } = renderInput();

    await userEvent.setup().type(input(), "France{Enter}");

    expect(props.submitGuess).toHaveBeenCalledWith("France");
  });

  it("submits the typed text with the Guess button", async () => {
    const user = userEvent.setup();
    const { props } = renderInput();

    await user.type(input(), "Spain");
    await user.click(screen.getByRole("button", { name: "Guess" }));

    expect(props.submitGuess).toHaveBeenCalledWith("Spain");
  });
});

describe("MapGameInput reveal", () => {
  it("needs two clicks to reveal the answer", async () => {
    const user = userEvent.setup();
    const { props } = renderInput();

    await user.click(screen.getByRole("button", { name: /Reveal answer/ }));
    expect(props.giveUp).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: /Tap again to reveal/ })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Tap again to reveal/ }));
    expect(props.giveUp).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: /Reveal answer/ })).toBeInTheDocument();
  });

  it("goes back to the first state after three seconds", () => {
    // Testing Library only detects Jest's fake timers, so user-event would hang here: use fireEvent.
    vi.useFakeTimers();
    const { props } = renderInput();

    fireEvent.click(screen.getByRole("button", { name: /Reveal answer/ }));
    act(() => {
      vi.advanceTimersByTime(3000);
    });

    expect(screen.getByRole("button", { name: /Reveal answer/ })).toBeInTheDocument();
    expect(props.giveUp).not.toHaveBeenCalled();
  });

  it("cleans up its timer when unmounted while waiting", () => {
    vi.useFakeTimers();
    const errors = vi.spyOn(console, "error").mockImplementation(() => {});
    const { unmount } = renderInput();

    fireEvent.click(screen.getByRole("button", { name: /Reveal answer/ }));
    unmount();

    expect(() => vi.advanceTimersByTime(5000)).not.toThrow();
    expect(vi.getTimerCount()).toBe(0);
    expect(errors).not.toHaveBeenCalled();
    errors.mockRestore();
  });
});

describe("MapGameInput hints", () => {
  const hints: HintConfig = { description: "Shows a clue" };

  it("shows how many hints are left and uses one on click", async () => {
    const { props } = renderInput({ hints, hintsUsed: 0, hintCount: 2 });

    await userEvent.setup().click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));

    expect(props.onHint).toHaveBeenCalledTimes(1);
  });

  it("disables the hint button when none are left", () => {
    renderInput({ hints, hintsUsed: 2, hintCount: 2 });

    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });

  it("shows what the round is worth, dropping with each hint", () => {
    const { rerender, props } = renderInput({ hints, hintsUsed: 0, hintCount: 3 });
    expect(screen.getByText("Worth 100 pts")).toBeInTheDocument();

    rerender(<MapGameInput {...props} hintsUsed={1} />);
    expect(screen.getByText("Worth 83 pts")).toBeInTheDocument();
    rerender(<MapGameInput {...props} hintsUsed={2} />);
    expect(screen.getByText("Worth 67 pts")).toBeInTheDocument();
    rerender(<MapGameInput {...props} hintsUsed={3} />);
    expect(screen.getByText("Worth 50 pts")).toBeInTheDocument();
  });

  it("puts the cost of the next hint in the button's title", () => {
    const { rerender, props } = renderInput({ hints, hintsUsed: 0, hintCount: 3 });
    // 100 -> 83 -> 67 -> 50: the drops are 17, 16 and 17, not 50 / 3.
    expect(screen.getByTitle("Shows a clue (−17 pts)")).toBeInTheDocument();

    rerender(<MapGameInput {...props} hintsUsed={1} />);
    expect(screen.getByTitle("Shows a clue (−16 pts)")).toBeInTheDocument();
    rerender(<MapGameInput {...props} hintsUsed={2} />);
    expect(screen.getByTitle("Shows a clue (−17 pts)")).toBeInTheDocument();
    rerender(<MapGameInput {...props} hintsUsed={3} />);
    expect(screen.getByTitle("No hints left")).toBeInTheDocument();
  });

  it("shows only the worth when there are no hints", () => {
    renderInput({ hints: undefined, hintCount: 0 });

    expect(screen.getByText("Worth 100 pts")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Hint/ })).not.toBeInTheDocument();
  });

  it("has no hint button without hints", () => {
    renderInput({ hints: undefined });

    expect(screen.queryByRole("button", { name: /Hint/ })).not.toBeInTheDocument();
  });
});

describe("MapGameInput states", () => {
  it("shows the round result instead of the form when the round is over", () => {
    renderInput({ gameOver: true, gaveUp: false });

    expect(screen.getByText("Perfect! +100")).toBeInTheDocument();
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Guess" })).not.toBeInTheDocument();
  });

  it("shows that the answer was revealed", () => {
    renderInput({ gameOver: true, gaveUp: true });

    expect(screen.getByText("Answer revealed · 0 pts")).toBeInTheDocument();
  });

  it("lists each wrong guess", () => {
    renderInput({ wrongGuesses: [{ text: "Italy", answer: null }, { text: "Peru", answer: null }] });

    expect(screen.getByText("Wrong guesses")).toBeInTheDocument();
    expect(screen.getByText("Italy")).toBeInTheDocument();
    expect(screen.getByText("Peru")).toBeInTheDocument();
  });

  it("shows no wrong guesses heading when there are none", () => {
    renderInput();

    expect(screen.queryByText("Wrong guesses")).not.toBeInTheDocument();
  });

  it("passes suggestions to the guess box as a datalist", () => {
    const { container } = renderInput({ suggestions: ["France", "Spain"] });

    const options = Array.from(container.querySelectorAll("datalist option")).map((o) => o.getAttribute("value"));
    expect(options).toEqual(["France", "Spain"]);
    // An input with a list attribute is exposed as a combobox rather than a textbox.
    expect(screen.getByRole("combobox", { name: "Country…" })).toHaveAttribute("list");
  });

  it("renders no suggestions list when none are given", () => {
    const { container } = renderInput();

    expect(container.querySelector("datalist")).toBeNull();
    expect(input()).not.toHaveAttribute("list");
  });
});
