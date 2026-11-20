import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { makeRoundProps } from "../../test/fixtures";
import RoundResult from "./RoundResult";

describe("RoundResult", () => {
  it("shows a spinner on a disabled next button while saving, then focuses it", () => {
    const props = makeRoundProps({ answerSave: "saving" });
    const { rerender } = render(<RoundResult gaveUp={false} round={props} hintsUsed={0} hintCount={3} />);

    expect(screen.getByRole("button")).toBeDisabled();
    expect(screen.getByRole("status")).toBeInTheDocument();

    rerender(<RoundResult gaveUp={false} round={{ ...props, answerSave: "saved" }} hintsUsed={0} hintCount={3} />);

    expect(screen.getByRole("button", { name: /Next round/ })).toHaveFocus();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("celebrates a first guess", () => {
    render(<RoundResult gaveUp={false} round={makeRoundProps()} hintsUsed={0} hintCount={3} />);

    expect(screen.getByText("First guess! +100")).toHaveClass("gg-text-correct");
    expect(screen.queryByText(/hints? used/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Answer revealed/)).not.toBeInTheDocument();
  });

  it("says no wrong guesses for a first guess in a game with several answers", () => {
    render(<RoundResult gaveUp={false} round={makeRoundProps()} hintsUsed={0} hintCount={3} answerCount={3} />);

    expect(screen.getByText("No wrong guesses! +100")).toBeInTheDocument();
  });

  it("calls a win after a wrong guess correct, not a first guess", () => {
    render(<RoundResult gaveUp={false} round={makeRoundProps()} hintsUsed={0} hintCount={3} wrongGuesses={1} />);

    expect(screen.getByText("Correct! +100")).toHaveClass("gg-text-correct");
    expect(screen.queryByText(/First guess/)).not.toBeInTheDocument();
  });

  it("scores a correct answer with a hint in yellow, without a separate hints line", () => {
    render(<RoundResult gaveUp={false} round={makeRoundProps()} hintsUsed={1} hintCount={3} />);

    expect(screen.getByText("Correct with a hint · +83")).toHaveClass("gg-text-hints");
    expect(screen.queryByText(/hints? used/)).not.toBeInTheDocument();
    expect(screen.queryByText(/First guess/)).not.toBeInTheDocument();
  });

  it("pluralises the hints", () => {
    render(<RoundResult gaveUp={false} round={makeRoundProps()} hintsUsed={2} hintCount={3} />);

    expect(screen.getByText("Correct with 2 hints · +67")).toBeInTheDocument();
  });

  it("says the answer was revealed, for no points, after giving up", () => {
    render(<RoundResult gaveUp round={makeRoundProps()} hintsUsed={2} hintCount={3} />);

    expect(screen.getByText("Answer revealed · 0 pts")).toBeInTheDocument();
    expect(screen.queryByText(/Correct/)).not.toBeInTheDocument();
    expect(screen.queryByText(/hints? used/)).not.toBeInTheDocument();
  });

  it("says the guesses ran out, for no points", () => {
    render(<RoundResult gaveUp outOfGuesses round={makeRoundProps()} hintsUsed={2} hintCount={3} />);

    expect(screen.getByText("Out of guesses · 0 pts")).toBeInTheDocument();
    expect(screen.queryByText(/Answer revealed/)).not.toBeInTheDocument();
  });

  it("offers the next round when there is one", async () => {
    const round = makeRoundProps({ hasNextRound: true });
    render(<RoundResult gaveUp={false} round={round} hintsUsed={0} hintCount={3} />);

    await userEvent.setup().click(screen.getByRole("button", { name: /Next round/ }));

    expect(round.nextRound).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("button", { name: "See results" })).not.toBeInTheDocument();
  });

  it("offers the results on the last round", async () => {
    const round = makeRoundProps({ hasNextRound: false });
    render(<RoundResult gaveUp={false} round={round} hintsUsed={0} hintCount={3} />);

    await userEvent.setup().click(screen.getByRole("button", { name: "See results" }));

    expect(round.showSummary).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("button", { name: /Next round/ })).not.toBeInTheDocument();
  });

  it("scores the share found of a round given up, and says how many were found", () => {
    render(
      <RoundResult gaveUp round={makeRoundProps()} hintsUsed={1} hintCount={3} answersFound={2} answerCount={3} />,
    );

    // Giving up with some answers found is not penalised: yellow, like a win with hints.
    expect(screen.getByText("Answer revealed · 2 of 3 found · +55")).toHaveClass("gg-text-hints");
    expect(screen.getByText("1 hint used")).toBeInTheDocument();
  });

  it("scores the share found of a round that ran out of guesses", () => {
    render(
      <RoundResult
        gaveUp
        outOfGuesses
        round={makeRoundProps()}
        hintsUsed={0}
        hintCount={3}
        answersFound={1}
        answerCount={2}
      />,
    );

    expect(screen.getByText("Out of guesses · 1 of 2 found · +50")).toBeInTheDocument();
  });

  it("shows red text when nothing was found", () => {
    render(
      <RoundResult gaveUp round={makeRoundProps()} hintsUsed={0} hintCount={3} answersFound={0} answerCount={3} />,
    );

    expect(screen.getByText("Answer revealed · 0 pts")).toHaveClass("gg-text-missed");
  });
});
