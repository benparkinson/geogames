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
    const { container } = await submitting({ kind: "outOfGuesses", guess: "Spain" });

    expect(container.querySelector(".guess-feedback")).toBeEmptyDOMElement();
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

  it("shows no feedback for a guess made after the round is over", async () => {
    const { container } = await submitting({ kind: "over" });

    expect(container.querySelector(".guess-feedback")).toBeEmptyDOMElement();
  });

  it("puts the feedback line right after the form, as its sibling", () => {
    const { container } = renderForm();

    const form = container.querySelector("form") as HTMLElement;
    expect(form.nextElementSibling).toHaveClass("guess-feedback");
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
});

describe("GuessForm suggestions", () => {
  it("passes suggestions to the guess box as a datalist", () => {
    const { container } = renderForm({ suggestions: ["France", "Spain"] });

    const options = Array.from(container.querySelectorAll("datalist option")).map((o) => o.getAttribute("value"));
    expect(options).toEqual(["France", "Spain"]);
    // An input with a list attribute is exposed as a combobox rather than a textbox.
    expect(screen.getByRole("combobox", { name: "Country…" })).toHaveAttribute("list");
  });

  it("renders no suggestions list when none are given", () => {
    const { container } = renderForm();

    expect(container.querySelector("datalist")).toBeNull();
    expect(input()).not.toHaveAttribute("list");
  });
});
