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

function renderGame(data = makeCountryShapeRoundData(), submitAnswer = vi.fn()) {
  const Component = neighbours.Component;
  mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
  renderWithClient(
    <Component data={data} round={makeRoundProps({ hintCount: 3 })} answerCount={1} submitAnswer={submitAnswer} />,
  );
  return submitAnswer;
}

describe("Country Gaps", () => {
  it("is the COUNTRIES_BY_NEIGHBOURS game", () => {
    expect(neighbours.type).toBe("COUNTRIES_BY_NEIGHBOURS");
    expect(neighbours.answerNames(makeCountryShapeRoundData())).toEqual(["Netherlands"]);
  });

  it("shows the prompt and the caption", () => {
    renderGame();

    expect(screen.getByText("Name the missing country.")).toBeInTheDocument();
    expect(screen.getByText("Only its neighbours are shown. The country is the gap.")).toBeInTheDocument();
  });

  it("offers three hints: the coastline on the map, the neighbours, then the first letter", async () => {
    const user = userEvent.setup();
    renderGame();

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    expect(screen.getByText("Its coastline is on the map")).toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-hints", "1");

    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    expect(screen.getByText("Borders: Germany, Belgium")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));
    expect(screen.getByText("Starts with “N”")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });

  it("says so when the country has no coastline", async () => {
    const user = userEvent.setup();
    renderGame(makeCountryShapeRoundData({ country: makeCountryShapeData({ coastKm: 0 }) }));

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));

    expect(screen.getByText("It has no coastline")).toBeInTheDocument();
  });

  it("accepts a correct guess by an alias", async () => {
    const user = userEvent.setup();
    const submitAnswer = renderGame();

    await user.type(screen.getByPlaceholderText("Country name…"), "Holland{Enter}");

    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "CORRECT", hintsUsed: 0 });
  });

  it("rejects an unknown name once the country list loads, without using a guess", async () => {
    const user = userEvent.setup();
    const submitAnswer = renderGame();
    await screen.findByRole("button", { name: /Hint/ });
    const left = () => screen.getByText(/guesses? left/i).textContent;
    const before = left();

    await user.type(screen.getByPlaceholderText("Country name…"), "Atlantis{Enter}");
    await user.type(screen.getByPlaceholderText("Country name…"), "{Enter}");

    expect(submitAnswer).not.toHaveBeenCalled();
    expect(left()).toBe(before);
  });
});
