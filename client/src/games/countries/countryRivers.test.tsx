import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { mockFetch } from "../../test/fetchMock";
import { countryNames, makeCountryRiversRoundData, makeCountryShapeData, makeRoundProps } from "../../test/fixtures";
import { renderWithClient } from "../../test/render";
import countryRivers from "./countryRivers";
import type { CountryRiversRound } from "./model";

vi.mock("./CountryRiversMapWrapper", () => ({
  default: ({ gameOver, hintsUsed }: { gameOver: boolean; hintsUsed: number }) => (
    <div data-testid="map" data-game-over={String(gameOver)} data-hints={hintsUsed} />
  ),
}));

function renderGame(data: CountryRiversRound = makeCountryRiversRoundData(), submitAnswer = vi.fn()) {
  const Component = countryRivers.Component;
  mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
  renderWithClient(
    <Component data={data} round={makeRoundProps({ hintCount: 3 })} answerCount={1} submitAnswer={submitAnswer} />,
  );
  return submitAnswer;
}

describe("Country Rivers", () => {
  it("is the COUNTRIES_BY_RIVERS game", () => {
    expect(countryRivers.type).toBe("COUNTRIES_BY_RIVERS");
    expect(countryRivers.extraDifficult).toBeUndefined();
    expect(countryRivers.answerNames(makeCountryRiversRoundData())).toEqual(["Netherlands"]);
  });

  it("shows the prompt and the map key", () => {
    renderGame();

    expect(screen.getByText("Which country has these rivers?")).toBeInTheDocument();
    expect(
      screen.getByText("The solid lines are rivers inside it. Thicker lines are major rivers."),
    ).toBeInTheDocument();
  });

  it("offers three hints: the lakes and the coastline on the map, then the first letter", async () => {
    const user = userEvent.setup();
    renderGame();

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    expect(screen.getByText("Its lakes are on the map")).toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-hints", "1");

    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    expect(screen.getByText("Its coastline is on the map")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));
    expect(screen.getByText("Starts with “N”")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });

  it("says so when a country has no large lakes or no coastline, which are clues in themselves", async () => {
    const user = userEvent.setup();
    renderGame(
      makeCountryRiversRoundData({
        country: makeCountryShapeData({ coastKm: 0 }),
        lakes: { type: "MultiPolygon", properties: {}, geometry: { type: "MultiPolygon", coordinates: [] } } as never,
      }),
    );

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));

    expect(screen.getByText("It has no large lakes")).toBeInTheDocument();
    expect(screen.getByText("It has no coastline")).toBeInTheDocument();
  });

  it("accepts a correct guess by an alias", async () => {
    const user = userEvent.setup();
    const submitAnswer = renderGame();

    await user.type(screen.getByPlaceholderText("Country name…"), "holland{Enter}");

    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "CORRECT", hintsUsed: 0, wrongGuesses: 0 });
  });
});
