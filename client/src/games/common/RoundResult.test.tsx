import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { makeRoundProps } from "../../test/fixtures";
import RoundResult from "./RoundResult";

describe("RoundResult", () => {
  it("congratulates a correct answer", () => {
    render(<RoundResult gaveUp={false} round={makeRoundProps()} />);

    expect(screen.getByText("Correct!")).toBeInTheDocument();
    expect(screen.queryByText("Answer revealed")).not.toBeInTheDocument();
  });

  it("says the answer was revealed after giving up", () => {
    render(<RoundResult gaveUp round={makeRoundProps()} />);

    expect(screen.getByText("Answer revealed")).toBeInTheDocument();
    expect(screen.queryByText("Correct!")).not.toBeInTheDocument();
  });

  it("offers the next round when there is one", async () => {
    const round = makeRoundProps({ hasNextRound: true });
    render(<RoundResult gaveUp={false} round={round} />);

    await userEvent.setup().click(screen.getByRole("button", { name: /Next round/ }));

    expect(round.nextRound).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("button", { name: "See results" })).not.toBeInTheDocument();
  });

  it("offers the results on the last round", async () => {
    const round = makeRoundProps({ hasNextRound: false });
    render(<RoundResult gaveUp={false} round={round} />);

    await userEvent.setup().click(screen.getByRole("button", { name: "See results" }));

    expect(round.showSummary).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("button", { name: /Next round/ })).not.toBeInTheDocument();
  });
});
