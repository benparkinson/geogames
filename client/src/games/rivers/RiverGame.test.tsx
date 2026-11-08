import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { makeRiverRoundData, makeRoundProps } from "../../test/fixtures";
import { makeRiverGame } from "./RiverGame";
import { continentHint, textHints } from "./riverHints";

const CAPTION = "Shape only. The satellite map appears when you answer.";

const RiverGame = makeRiverGame({
  MapComponent: ({ gameOver, gaveUp }: { gameOver: boolean; gaveUp: boolean }) => (
    <div data-testid="map" data-game-over={String(gameOver)} data-gave-up={String(gaveUp)} />
  ),
  prompt: "Name this river from its shape.",
  blankMap: true,
  mapCaption: CAPTION,
  hints: (river) => textHints(river, continentHint),
});

function renderRiver() {
  const submitAnswer = vi.fn();
  render(<RiverGame data={makeRiverRoundData()} round={makeRoundProps({ hintCount: 1 })} answerCount={1} submitAnswer={submitAnswer} />);
  return { submitAnswer };
}

const input = () => screen.getByRole("textbox", { name: "River name…" });

describe("makeRiverGame with the shape configuration", () => {
  it("asks for the river's name", () => {
    renderRiver();

    expect(screen.getByText("Name this river from its shape.")).toBeInTheDocument();
  });

  it.each(["Danube", "danube", "  DANUBE "])("accepts %j", async (text) => {
    const { submitAnswer } = renderRiver();

    await userEvent.setup().type(input(), text + "{Enter}");

    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "CORRECT", hintsUsed: 0 });
  });

  it("accepts an alias", async () => {
    const { submitAnswer } = renderRiver();

    await userEvent.setup().type(input(), "Donau{Enter}");

    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "CORRECT", hintsUsed: 0 });
  });

  it("rejects another river", async () => {
    const { submitAnswer } = renderRiver();

    await userEvent.setup().type(input(), "Rhine{Enter}");

    expect(screen.getByText('"Rhine" isn\'t right. 4 guesses left.')).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
  });

  it("offers a single hint that names the continent", async () => {
    const user = userEvent.setup();
    renderRiver();

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));

    expect(screen.getByText("Continent: Europe")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });

  it("hides the satellite map until the round ends", async () => {
    renderRiver();
    expect(screen.getByText(CAPTION)).toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-game-over", "false");

    await userEvent.setup().type(input(), "Danube{Enter}");

    expect(screen.queryByText(CAPTION)).not.toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-game-over", "true");
  });
});
