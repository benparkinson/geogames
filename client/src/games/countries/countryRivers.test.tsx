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

  it("shows the prompt", () => {
    renderGame();

    expect(screen.getByText("Which country has these rivers?")).toBeInTheDocument();
  });

  it("offers three hints: the longest river, the coastline on the map, then the first letter", async () => {
    const user = userEvent.setup();
    renderGame();

    await user.click(screen.getByRole("button", { name: /Hint \(.*, 3 left\)/ }));
    expect(screen.getByText("Its longest river is the Rhine")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint \(.*, 2 left\)/ }));
    expect(screen.getByText("Its coastline is on the map")).toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-hints", "2");

    await user.click(screen.getByRole("button", { name: /Hint \(.*, 1 left\)/ }));
    expect(screen.getByText("Starts with “N”")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });

  it("gives the length of its name first when the country has no named river, and says it has no coastline", async () => {
    const user = userEvent.setup();
    renderGame(
      makeCountryRiversRoundData({
        country: makeCountryShapeData({ coastKm: 0 }),
        longestRiver: null,
      }),
    );

    await user.click(screen.getByRole("button", { name: /Hint \(.*, 3 left\)/ }));
    expect(screen.getByText("Its name has 11 letters")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Hint \(.*, 2 left\)/ }));

    expect(screen.getByText("It has no coastline")).toBeInTheDocument();
  });

  it("accepts a correct guess by an alias", async () => {
    const user = userEvent.setup();
    const submitAnswer = renderGame();

    await user.type(screen.getByPlaceholderText("Country name…"), "holland{Enter}");

    expect(submitAnswer).toHaveBeenCalledWith({
      answerState: "CORRECT",
      hintsUsed: 0,
      wrongGuesses: 0,
      answersFound: 1,
    });
  });
});
