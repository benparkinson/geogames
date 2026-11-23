import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { mockFetch } from "../../test/fetchMock";
import { makeRiverRoundData, makeRoundProps, riverNames } from "../../test/fixtures";
import { renderWithClient } from "../../test/render";
import countries from "./countries";

vi.mock("./RiverCountriesMapWrapper", () => ({
  default: ({ gameOver, hintsUsed }: { gameOver: boolean; hintsUsed: number }) => (
    <div data-testid="map" data-game-over={String(gameOver)} data-hints-used={hintsUsed} />
  ),
}));

function renderGame() {
  const submitAnswer = vi.fn();
  const Component = countries.Component;
  mockFetch([{ url: /\/api\/rivers$/, body: riverNames }]);
  renderWithClient(
    <Component
      data={makeRiverRoundData()}
      round={makeRoundProps({ hintCount: 2 })}
      answerCount={1}
      submitAnswer={submitAnswer}
    />,
  );
  return { submitAnswer };
}

const input = () => screen.getByLabelText("River name…");

describe("River Countries", () => {
  it("is the RIVERS_BY_COUNTRIES game", () => {
    expect(countries.type).toBe("RIVERS_BY_COUNTRIES");
    expect(countries.title).toBe("River Countries");
    expect(countries.answers(makeRiverRoundData()).map((a) => a.name)).toEqual(["Danube"]);
  });

  it("asks for the river and blanks the map until the round ends", async () => {
    renderGame();
    expect(screen.getByText("Which river flows through all of these countries?")).toBeInTheDocument();
    expect(screen.getByRole("main")).toHaveAttribute("data-blank-map");

    await userEvent.setup().type(input(), "Danube{Enter}");

    expect(screen.getByRole("main")).not.toHaveAttribute("data-blank-map");
    expect(screen.getByTestId("map")).toHaveAttribute("data-game-over", "true");
  });

  it("first hint marks the start and end on the map, second gives the first letter", async () => {
    const user = userEvent.setup();
    renderGame();

    await user.click(screen.getByRole("button", { name: /Hint \(.*, 2 left\)/ }));
    expect(screen.getByText("The start and end are marked on the map")).toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-hints-used", "1");

    await user.click(screen.getByRole("button", { name: /Hint \(.*, 1 left\)/ }));
    expect(screen.getByText("Starts with “D”")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });
});
