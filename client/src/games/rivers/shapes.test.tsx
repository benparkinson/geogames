import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { mockFetch } from "../../test/fetchMock";
import { makeRiverRoundData, makeRoundProps, riverNames } from "../../test/fixtures";
import { renderWithClient } from "../../test/render";
import shapes from "./shapes";

vi.mock("./RiverShapesMapWrapper", () => ({
  default: ({ gameOver }: { gameOver: boolean }) => <div data-testid="map" data-game-over={String(gameOver)} />,
}));

describe("River Shapes", () => {
  it("is the RIVERS_BY_SHAPE game", () => {
    expect(shapes.type).toBe("RIVERS_BY_SHAPE");
    expect(shapes.answers(makeRiverRoundData()).map((a) => a.name)).toEqual(["Danube"]);
  });

  it("offers three hints, weakest first: continent, countries, then the first letter", async () => {
    const user = userEvent.setup();
    const Component = shapes.Component;
    mockFetch([{ url: /\/api\/rivers$/, body: riverNames }]);
    renderWithClient(
      <Component
        data={makeRiverRoundData()}
        round={makeRoundProps({ hintCount: 3 })}
        answerCount={1}
        submitAnswer={vi.fn()}
      />,
    );

    await user.click(screen.getByRole("button", { name: /Hint .*· 3 left/ }));
    expect(screen.getByText("Continent: Europe")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint .*· 2 left/ }));
    expect(screen.getByText("Its longest stretch is in Romania")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint .*· 1 left/ }));
    expect(screen.getByText("Starts with “D”")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint · 0 left/ })).toBeDisabled();
  });

  it("offers two hints in Easy, where the continent is already shown, so the first is the countries", async () => {
    const user = userEvent.setup();
    const Component = shapes.Component;
    mockFetch([{ url: /\/api\/rivers$/, body: riverNames }]);
    renderWithClient(
      <Component
        data={makeRiverRoundData()}
        round={makeRoundProps({ hintCount: 2, difficulty: "EASY", continents: ["Europe"] })}
        answerCount={1}
        submitAnswer={vi.fn()}
      />,
    );
    // The continent is shown from the start, as the first line of the list, and not as a hint.
    expect(screen.getByText("Continent: Europe")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint .*· 2 left/ }));
    expect(screen.getByText("Its longest stretch is in Romania")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Hint .*· 1 left/ }));
    expect(screen.getByText("Starts with “D”")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint · 0 left/ })).toBeDisabled();
    expect(screen.getAllByText(/^Continent/)).toHaveLength(1);
  });

  it.each([
    ["NORMAL", true],
    ["HARD", false],
  ] as const)("in %s mode a wrong guess shows a distance clue: %s", async (difficulty, shown) => {
    const user = userEvent.setup();
    const Component = shapes.Component;
    mockFetch([{ url: /\/api\/rivers$/, body: riverNames }]);
    renderWithClient(
      <Component
        data={makeRiverRoundData()}
        round={makeRoundProps({ hintCount: 3, hasClues: true, difficulty })}
        answerCount={1}
        submitAnswer={vi.fn()}
      />,
    );
    await screen.findByRole("button", { name: /Hint/ });

    await user.type(screen.getByPlaceholderText("River name…"), "Thames{Enter}");

    expect(await screen.findByText("Thames")).toBeInTheDocument();
    if (shown) {
      expect(screen.getByText(/[\d,]+ km (north|south|east|west)/)).toBeInTheDocument();
    } else {
      expect(screen.queryByText(/ km/)).not.toBeInTheDocument();
    }
  });

  it("has clues", () => {
    expect(shapes.hasClues).toBe(true);
  });
});
