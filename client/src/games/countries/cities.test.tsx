import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { mockFetch } from "../../test/fetchMock";
import { countryNames, makeCountryCitiesRoundData, makeCountryShapeData, makeRoundProps } from "../../test/fixtures";
import { renderWithClient } from "../../test/render";
import cities from "./cities";
import type { CountryCitiesRound } from "./model";

vi.mock("./CountryCitiesMapWrapper", () => ({
  default: ({ gameOver, hintsUsed }: { gameOver: boolean; hintsUsed: number }) => (
    <div data-testid="map" data-game-over={String(gameOver)} data-hints={hintsUsed} />
  ),
}));

function renderGame(data: CountryCitiesRound = makeCountryCitiesRoundData(), submitAnswer = vi.fn()) {
  const Component = cities.Component;
  mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
  renderWithClient(
    <Component data={data} round={makeRoundProps({ hintCount: 3 })} answerCount={1} submitAnswer={submitAnswer} />,
  );
  return submitAnswer;
}

describe("Country Cities", () => {
  it("is the COUNTRIES_BY_CITIES game", () => {
    expect(cities.type).toBe("COUNTRIES_BY_CITIES");
    expect(cities.extraDifficult).toBeUndefined();
    expect(cities.answerNames(makeCountryCitiesRoundData())).toEqual(["Netherlands"]);
  });

  it("shows the prompt and the caption", () => {
    renderGame();

    expect(screen.getByText("Name the country with these cities.")).toBeInTheDocument();
    expect(screen.getByText("Its cities, sized by population. The outline is hidden.")).toBeInTheDocument();
  });

  it("offers three hints: the capital and the coastline on the map, then the first letter", async () => {
    const user = userEvent.setup();
    renderGame();

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    expect(screen.getByText("Its capital is highlighted")).toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-hints", "1");

    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    expect(screen.getByText("Its coastline is on the map")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));
    expect(screen.getByText("Starts with “N”")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });

  it("says so when a country has no coastline, which is itself a clue", async () => {
    const user = userEvent.setup();
    renderGame(makeCountryCitiesRoundData({ country: makeCountryShapeData({ coastKm: 0 }) }));

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));

    expect(screen.getByText("It has no coastline")).toBeInTheDocument();
  });

  it("accepts a correct guess by an alias", async () => {
    const user = userEvent.setup();
    const submitAnswer = renderGame();

    await user.type(screen.getByPlaceholderText("Country name…"), "holland{Enter}");

    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "CORRECT", hintsUsed: 0, wrongGuesses: 0 });
  });
});
