import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { mockFetch } from "../../test/fetchMock";
import { countryNames, makeCountryShapeData, makeCountryShapeRoundData, makeRoundProps } from "../../test/fixtures";
import { renderWithClient } from "../../test/render";
import neighbours from "./neighbours";

vi.mock("./NeighboursMapWrapper", () => ({
  default: ({ gameOver, hintsUsed }: { gameOver: boolean; hintsUsed: number }) => (
    <div data-testid="map" data-game-over={String(gameOver)} data-hints={hintsUsed} />
  ),
}));

function renderGame(
  data = makeCountryShapeRoundData(),
  submitAnswer = vi.fn(),
  difficulty: "NORMAL" | "HARD" = "NORMAL",
) {
  const Component = neighbours.Component;
  mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
  renderWithClient(
    <Component
      data={data}
      round={makeRoundProps({ hintCount: 3, hasClues: true, difficulty })}
      answerCount={1}
      submitAnswer={submitAnswer}
    />,
  );
  return submitAnswer;
}

describe("Missing Country", () => {
  it("is the COUNTRIES_BY_NEIGHBOURS game", () => {
    expect(neighbours.type).toBe("COUNTRIES_BY_NEIGHBOURS");
    expect(neighbours.answers(makeCountryShapeRoundData()).map((a) => a.name)).toEqual(["Netherlands"]);
  });

  it("shows the prompt", () => {
    renderGame();

    expect(screen.getByText("Which country is missing?")).toBeInTheDocument();
  });

  it("offers three hints: the coastline on the map, the longest border, then the first letter", async () => {
    const user = userEvent.setup();
    renderGame();

    await user.click(screen.getByRole("button", { name: /Hint .*· 3 left/ }));
    expect(screen.getByText("Its coastline is on the map")).toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-hints", "1");

    await user.click(screen.getByRole("button", { name: /Hint .*· 2 left/ }));
    expect(screen.getByText("Longest border: Germany")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint .*· 1 left/ }));
    expect(screen.getByText("Starts with “N”")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint · 0 left/ })).toBeDisabled();
  });

  it("gives the length of its name instead when the country has no coastline", async () => {
    const user = userEvent.setup();
    renderGame(makeCountryShapeRoundData({ country: makeCountryShapeData({ coastKm: 0 }) }));

    await user.click(screen.getByRole("button", { name: /Hint .*· 3 left/ }));

    expect(screen.getByText("Its name has 11 letters")).toBeInTheDocument();
  });

  it("accepts a correct guess by an alias", async () => {
    const user = userEvent.setup();
    const submitAnswer = renderGame();

    await user.type(screen.getByPlaceholderText("Country name…"), "Holland{Enter}");

    expect(submitAnswer).toHaveBeenCalledWith({
      answerState: "CORRECT",
      hintsUsed: 0,
      wrongGuesses: 0,
      answersFound: 1,
    });
  });

  it("tells a wrong guess how far and which way the answer is", async () => {
    const user = userEvent.setup();
    renderGame();
    await screen.findByRole("button", { name: /Hint/ });

    await user.type(screen.getByPlaceholderText("Country name…"), "France{Enter}");

    expect(screen.getByText(/[\d,]+ km (north|south|east|west)/)).toBeInTheDocument();
  });

  it("gives no clue in Hard mode", async () => {
    const user = userEvent.setup();
    renderGame(makeCountryShapeRoundData(), vi.fn(), "HARD");
    await screen.findByRole("button", { name: /Hint/ });

    await user.type(screen.getByPlaceholderText("Country name…"), "France{Enter}");

    expect(screen.getByText("France")).toBeInTheDocument();
    expect(screen.queryByText(/ km/)).not.toBeInTheDocument();
  });

  it("rejects an unknown name once the country list loads, without using a guess", async () => {
    const user = userEvent.setup();
    const submitAnswer = renderGame();
    await screen.findByRole("button", { name: /Hint/ });
    const left = () => screen.getByText(/(lives? left|Last life)/i).textContent;
    const before = left();

    await user.type(screen.getByPlaceholderText("Country name…"), "Atlantis{Enter}");
    await user.type(screen.getByPlaceholderText("Country name…"), "{Enter}");

    expect(submitAnswer).not.toHaveBeenCalled();
    expect(left()).toBe(before);
  });
});
