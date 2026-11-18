import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { mockFetch } from "../../test/fetchMock";
import { countryNames, makeCountryPairRoundData, makeRoundProps } from "../../test/fixtures";
import { renderWithClient } from "../../test/render";
import mergedCountries from "./mergedCountries";

vi.mock("./MergedCountriesMapWrapper", () => ({
  default: ({ gameOver, hintsUsed }: { gameOver: boolean; hintsUsed: number }) => (
    <div data-testid="map" data-game-over={String(gameOver)} data-hints={hintsUsed} />
  ),
}));

function renderGame(submitAnswer = vi.fn()) {
  const Component = mergedCountries.Component;
  mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
  renderWithClient(
    <Component
      data={makeCountryPairRoundData()}
      round={makeRoundProps({ hintCount: 3 })}
      answerCount={2}
      submitAnswer={submitAnswer}
    />,
  );
  return submitAnswer;
}

describe("Merged Countries", () => {
  it("is the MERGED_COUNTRIES game, in the standard section", () => {
    expect(mergedCountries.type).toBe("MERGED_COUNTRIES");
    expect(mergedCountries.extraDifficult).toBeUndefined();
    expect(mergedCountries.answerNames(makeCountryPairRoundData())).toEqual(["Belgium", "Netherlands"]);
  });

  it("shows the prompt", () => {
    renderGame();

    expect(screen.getByText("Which two neighbouring countries make this shape?")).toBeInTheDocument();
  });

  it("offers three hints: the surroundings and the border on the map, then both first letters", async () => {
    const user = userEvent.setup();
    renderGame();

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    expect(screen.getByText("The surrounding countries are on the map")).toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-hints", "1");

    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    expect(screen.getByText("The border between them is on the map")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));
    expect(screen.getByText("Their names start with “B” and “N”")).toBeInTheDocument();
  });

  it("finishes the round when both countries are named", async () => {
    const user = userEvent.setup();
    const submitAnswer = renderGame();

    await user.type(screen.getByPlaceholderText("Country name…"), "belgium{Enter}");
    await user.type(screen.getByPlaceholderText("Country name…"), "netherlands{Enter}");

    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "CORRECT", hintsUsed: 0, wrongGuesses: 0 });
  });
});
