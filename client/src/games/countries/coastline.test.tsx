import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { mockFetch } from "../../test/fetchMock";
import { countryNames, makeCountryShapeRoundData, makeRoundProps } from "../../test/fixtures";
import { renderWithClient } from "../../test/render";
import coastline from "./coastline";

vi.mock("./CoastlineMapWrapper", () => ({
  default: ({ gameOver, hintsUsed }: { gameOver: boolean; hintsUsed: number }) => (
    <div data-testid="map" data-game-over={String(gameOver)} data-hints={hintsUsed} />
  ),
}));

function renderGame(submitAnswer = vi.fn()) {
  const Component = coastline.Component;
  mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
  renderWithClient(
    <Component
      data={makeCountryShapeRoundData()}
      round={makeRoundProps({ hintCount: 3 })}
      answerCount={1}
      submitAnswer={submitAnswer}
    />,
  );
  return submitAnswer;
}

describe("Coastlines", () => {
  it("is the COUNTRIES_BY_COASTLINE game", () => {
    expect(coastline.type).toBe("COUNTRIES_BY_COASTLINE");
    expect(coastline.answerNames(makeCountryShapeRoundData())).toEqual(["Netherlands"]);
  });

  it("shows the prompt and the caption", () => {
    renderGame();

    expect(screen.getByText("Name the country with this coastline.")).toBeInTheDocument();
    expect(screen.getByText("Coastline only. Land borders are hidden.")).toBeInTheDocument();
  });

  it("offers three hints: the neighbour count, the land borders on the map, then the first letter", async () => {
    const user = userEvent.setup();
    renderGame();

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    expect(screen.getByText("Borders 3 countries")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    expect(screen.getByText("Its land borders are on the map")).toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-hints", "2");

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));
    expect(screen.getByText("Starts with “N”")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });

  it("accepts a correct guess by an alias", async () => {
    const user = userEvent.setup();
    const submitAnswer = renderGame();

    await user.type(screen.getByPlaceholderText("Country name…"), "holland{Enter}");

    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "CORRECT", hintsUsed: 0 });
  });
});
