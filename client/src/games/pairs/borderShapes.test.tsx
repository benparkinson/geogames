import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { mockFetch } from "../../test/fetchMock";
import { countryNames, makeCountryPairRoundData, makeRoundProps } from "../../test/fixtures";
import { renderWithClient } from "../../test/render";
import borderShapes from "./borderShapes";

vi.mock("./BorderShapesMapWrapper", () => ({
  default: ({ gameOver, hintsUsed }: { gameOver: boolean; hintsUsed: number }) => (
    <div data-testid="map" data-game-over={String(gameOver)} data-hints={hintsUsed} />
  ),
}));

function renderGame(submitAnswer = vi.fn()) {
  const Component = borderShapes.Component;
  mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
  renderWithClient(
    <Component
      data={makeCountryPairRoundData()}
      round={makeRoundProps({ hintCount: 3, hasClues: true })}
      answerCount={2}
      submitAnswer={submitAnswer}
    />,
  );
  return submitAnswer;
}

describe("Border Shapes", () => {
  it("is the BORDERS_BY_SHAPE game, in the extra difficult section", () => {
    expect(borderShapes.type).toBe("BORDERS_BY_SHAPE");
    expect(borderShapes.extraDifficult).toBe(true);
    expect(borderShapes.answerNames(makeCountryPairRoundData())).toEqual(["Belgium", "Netherlands"]);
  });

  it("shows the prompt", () => {
    renderGame();

    expect(screen.getByText("Which two countries does this border separate?")).toBeInTheDocument();
  });

  it("offers three hints: the other borders and the coastlines on the map, then both first letters", async () => {
    const user = userEvent.setup();
    renderGame();

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    expect(screen.getByText("The rest of both countries' land borders are on the map")).toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-hints", "1");

    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    expect(screen.getByText("Both coastlines are on the map")).toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-hints", "2");

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));
    expect(screen.getByText("Their names start with “B” and “N”")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });

  it("finishes the round when both countries are named, one by an alias", async () => {
    const user = userEvent.setup();
    const submitAnswer = renderGame();

    await user.type(screen.getByPlaceholderText("Country name…"), "holland{Enter}");
    expect(submitAnswer).not.toHaveBeenCalled();
    await user.type(screen.getByPlaceholderText("Country name…"), "belgium{Enter}");

    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "CORRECT", hintsUsed: 0, wrongGuesses: 0 });
  });

  it("says how far the nearest country you still need is after a wrong guess", async () => {
    const user = userEvent.setup();
    renderGame();
    await screen.findByRole("button", { name: /Hint/ });

    await user.type(screen.getByPlaceholderText("Country name…"), "France{Enter}");

    expect(screen.getByText(/[\d,]+ km (north|south|east|west)/)).toBeInTheDocument();
  });
});
