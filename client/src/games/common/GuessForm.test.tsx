import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import GuessForm from "./GuessForm";
import { GuessResult } from "./guessReducer";

function renderForm(overrides: Partial<React.ComponentProps<typeof GuessForm>> = {}) {
  const props: React.ComponentProps<typeof GuessForm> = {
    submitGuess: vi.fn<(guess: string) => GuessResult>(() => ({ kind: "over" })),
    inputPlaceholder: "Country…",
    answerNoun: "country",
    ...overrides,
  };
  const view = render(<GuessForm {...props} />);
  return { ...view, props };
}

const input = () => screen.getByRole("textbox", { name: "Country…" });

describe("GuessForm feedback", () => {
  async function submitting(result: GuessResult, text = "Spain") {
    const user = userEvent.setup();
    const view = renderForm({ submitGuess: vi.fn(() => result) });
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

  it("clears the text after a wrong guess and leaves the saying to the wrong-guess list", async () => {
    await submitting({ kind: "wrong", guess: "Spain", guessesLeft: 2 });

    expect(screen.queryByText(/Spain/)).not.toBeInTheDocument();
    expect(input()).toHaveValue("");
  });

  it("clears the text when the guesses have run out, and leaves the saying to RoundResult", async () => {
    await submitting({ kind: "outOfGuesses", guess: "Spain" });

    expect(screen.getByTestId("guess-feedback")).toBeEmptyDOMElement();
    expect(input()).toHaveValue("");
  });

  it("shows no message after a correct guess", async () => {
    await submitting({ kind: "correct", remaining: 2 });

    expect(screen.queryByText(/Correct/)).not.toBeInTheDocument();
    expect(input()).toHaveValue("");
  });

  it("shows no feedback for a guess made after the round is over", async () => {
    await submitting({ kind: "over" });

    expect(screen.getByTestId("guess-feedback")).toBeEmptyDOMElement();
  });

  it("puts the feedback line right after the form, as its sibling", () => {
    const { container } = renderForm();

    const form = container.querySelector("form") as HTMLElement;
    expect(form.nextElementSibling).toBe(screen.getByTestId("guess-feedback"));
  });
});

describe("GuessForm submitting", () => {
  it("submits the typed text on Enter", async () => {
    const { props } = renderForm();

    await userEvent.setup().type(input(), "France{Enter}");

    expect(props.submitGuess).toHaveBeenCalledWith("France");
  });

  it("submits the typed text with the Guess button", async () => {
    const user = userEvent.setup();
    const { props } = renderForm();

    await user.type(input(), "Spain");
    await user.click(screen.getByRole("button", { name: "Guess" }));

    expect(props.submitGuess).toHaveBeenCalledWith("Spain");
  });

  it("keeps the focus in the box when the Guess button is pressed, so a phone keeps its keyboard", async () => {
    const user = userEvent.setup();
    renderForm({ submitGuess: vi.fn(() => ({ kind: "correct", remaining: 1 }) as GuessResult) });

    await user.type(input(), "Spain");
    await user.click(screen.getByRole("button", { name: "Guess" }));

    expect(input()).toHaveFocus();
  });

  it("keeps the same box after wrong guesses, replaying the shake each time", async () => {
    const user = userEvent.setup();
    renderForm({ submitGuess: vi.fn(() => ({ kind: "wrong", guess: "x", guessesLeft: 2 }) as GuessResult) });
    const box = input();

    await user.type(box, "Spain{Enter}");
    expect(box).toHaveAttribute("data-shake", "1");
    await user.type(box, "Italy{Enter}");

    expect(input()).toBe(box);
    expect(box).toHaveAttribute("data-shake", "2");
    expect(box).toHaveFocus();
  });

  it("puts the focus back in the box after a guess sent from the keyboard-focused button", async () => {
    const user = userEvent.setup();
    renderForm();

    await user.type(input(), "Spain");
    screen.getByRole("button", { name: "Guess" }).focus();
    await user.keyboard("{Enter}");

    expect(input()).toHaveFocus();
  });
});

describe("GuessForm suggestions", () => {
  it("passes suggestions to the guess box", async () => {
    renderForm({
      suggestions: [
        { name: "France", aliases: [] },
        { name: "Spain", aliases: [] },
      ],
    });

    await userEvent.setup().type(screen.getByRole("combobox", { name: "Country…" }), "s");

    expect(screen.getAllByRole("option").map((o) => o.textContent)).toEqual(["Spain"]);
  });

  it("fills the box with a picked suggestion without submitting", async () => {
    const { props } = renderForm({ suggestions: [{ name: "France", aliases: [] }] });
    const user = userEvent.setup();

    await user.type(screen.getByRole("combobox"), "fra");
    await user.click(screen.getByRole("option", { name: "France" }));

    expect(screen.getByRole("combobox")).toHaveValue("France");
    expect(props.submitGuess).not.toHaveBeenCalled();
  });

  it("closes the suggestions when the guess is submitted", async () => {
    renderForm({
      suggestions: [{ name: "France", aliases: [] }],
      submitGuess: vi.fn<(guess: string) => GuessResult>(() => ({ kind: "unknown", guess: "fra" })),
    });
    const user = userEvent.setup();

    await user.type(screen.getByRole("combobox"), "fra");
    expect(screen.getByRole("listbox")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Guess" }));

    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  });

  it("has no suggestion list when none are given", async () => {
    renderForm();

    await userEvent.setup().type(input(), "f");

    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  });
});
