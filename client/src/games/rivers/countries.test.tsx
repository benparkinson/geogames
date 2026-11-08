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
  renderWithClient(<Component data={makeRiverRoundData()} round={makeRoundProps({ hintCount: 2 })} answerCount={1} submitAnswer={submitAnswer} />);
  return { submitAnswer };
}

const input = () => screen.getByLabelText("River name…");

describe("River Countries", () => {
  it("is the RIVERS_BY_COUNTRIES game", () => {
    expect(countries.type).toBe("RIVERS_BY_COUNTRIES");
    expect(countries.title).toBe("River Countries");
    expect(countries.answerNames(makeRiverRoundData())).toEqual(["Danube"]);
  });

  it("asks for the river and blanks the map until the round ends", async () => {
    renderGame();
    expect(screen.getByText("Name the river that flows through all of these countries.")).toBeInTheDocument();
    expect(document.querySelector(".game-map--blank")).not.toBeNull();

    await userEvent.setup().type(input(), "Danube{Enter}");

    expect(document.querySelector(".game-map--blank")).toBeNull();
    expect(screen.getByTestId("map")).toHaveAttribute("data-game-over", "true");
  });

  it("first hint puts the flags on the map, second gives the first letter", async () => {
    const user = userEvent.setup();
    renderGame();

    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    expect(screen.getByText("Start and end flags are on the map")).toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-hints-used", "1");

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));
    expect(screen.getByText("Starts with “D”")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });
});
