import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { makeRoundProps } from "../../test/fixtures";
import RoundResult from "./RoundResult";

describe("RoundResult", () => {
  it("celebrates a perfect answer", () => {
    render(<RoundResult gaveUp={false} round={makeRoundProps()} hintsUsed={0} hintCount={3} />);

    expect(screen.getByText("Perfect! +100")).toBeInTheDocument();
    expect(screen.queryByText(/hints? used/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Answer revealed/)).not.toBeInTheDocument();
  });

  it("scores a correct answer with hints and says how many were used", () => {
    render(<RoundResult gaveUp={false} round={makeRoundProps()} hintsUsed={1} hintCount={3} />);

    expect(screen.getByText("Correct! +83")).toBeInTheDocument();
    expect(screen.getByText("1 hint used")).toBeInTheDocument();
    expect(screen.queryByText(/Perfect/)).not.toBeInTheDocument();
  });

  it("pluralises the hints used", () => {
    render(<RoundResult gaveUp={false} round={makeRoundProps()} hintsUsed={2} hintCount={3} />);

    expect(screen.getByText("Correct! +67")).toBeInTheDocument();
    expect(screen.getByText("2 hints used")).toBeInTheDocument();
  });

  it("says the answer was revealed, for no points, after giving up", () => {
    render(<RoundResult gaveUp round={makeRoundProps()} hintsUsed={2} hintCount={3} />);

    expect(screen.getByText("Answer revealed · 0 pts")).toBeInTheDocument();
    expect(screen.queryByText(/Correct/)).not.toBeInTheDocument();
    expect(screen.queryByText(/hints? used/)).not.toBeInTheDocument();
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
});
