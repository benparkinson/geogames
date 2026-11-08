import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { mockFetch } from "../../test/fetchMock";
import { makeRiverRoundData, makeRoundProps, riverNames } from "../../test/fixtures";
import { renderWithClient } from "../../test/render";
import onMap from "./onMap";

vi.mock("./RiverOnMapMapWrapper", () => ({
  default: ({ gameOver }: { gameOver: boolean }) => <div data-testid="map" data-game-over={String(gameOver)} />,
}));

function renderGame() {
  const submitAnswer = vi.fn();
  const Component = onMap.Component;
  mockFetch([{ url: /\/api\/rivers$/, body: riverNames }]);
  renderWithClient(<Component data={makeRiverRoundData()} round={makeRoundProps({ hintCount: 2 })} answerCount={1} submitAnswer={submitAnswer} />);
  return { submitAnswer };
}

const input = () => screen.getByLabelText("River name…");

describe("Rivers on the Map", () => {
  it("is the RIVERS_ON_MAP game", () => {
    expect(onMap.type).toBe("RIVERS_ON_MAP");
    expect(onMap.title).toBe("Rivers on the Map");
    expect(onMap.answerNames(makeRiverRoundData())).toEqual(["Danube"]);
  });

  it("asks for the river and explains the limited zoom until the round ends", async () => {
    renderGame();
    expect(screen.getByText("Name the river shown on the map.")).toBeInTheDocument();
    expect(screen.getByText("No panning: zoom is limited.")).toBeInTheDocument();

    await userEvent.setup().type(input(), "Danube{Enter}");

    expect(screen.queryByText("No panning: zoom is limited.")).not.toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-game-over", "true");
  });

  it("offers two hints in order: the countries, then the first letter", async () => {
    const user = userEvent.setup();
    renderGame();

    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    expect(screen.getByText("Flows through: Romania, Germany, Austria")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));
    expect(screen.getByText("Starts with “D”")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });

  it("accepts the name or an alias", async () => {
    const { submitAnswer } = renderGame();

    await userEvent.setup().type(input(), "Donau{Enter}");

    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "CORRECT", hintsUsed: 0 });
  });
});
