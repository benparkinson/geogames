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
    expect(shapes.answerNames(makeRiverRoundData())).toEqual(["Danube"]);
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

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    expect(screen.getByText("Continent: Europe")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    expect(screen.getByText("Flows through: Romania, Germany, Austria")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));
    expect(screen.getByText("Starts with “D”")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });
});
