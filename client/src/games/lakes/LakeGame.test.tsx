import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { mockFetch } from "../../test/fetchMock";
import { lakeNames, makeLakeData, makeLakeRoundData, makeRoundProps } from "../../test/fixtures";
import { renderWithClient } from "../../test/render";
import LakeGame from "./LakeGame";

vi.mock("./LakeShapesMapWrapper", () => ({
  default: ({ gameOver, gaveUp }: { gameOver: boolean; gaveUp: boolean }) => (
    <div data-testid="map" data-game-over={String(gameOver)} data-gave-up={String(gaveUp)} />
  ),
}));

/** Renders the game and waits for the lake names to load. */
async function renderLake(data = makeLakeRoundData()) {
  const submitAnswer = vi.fn();
  mockFetch([{ url: /\/api\/lakes$/, body: lakeNames }]);
  const { client } = renderWithClient(<LakeGame data={data} round={makeRoundProps({ hintCount: 3 })} answerCount={1} submitAnswer={submitAnswer} />);
  await waitFor(() => expect(client.getQueryData(["placeNames", "lakes"])).toBeDefined());
  return { submitAnswer };
}

const input = () => screen.getByLabelText("Lake name…");

describe("LakeGame", () => {
  it("shows the prompt and the caption", async () => {
    await renderLake();

    expect(screen.getByText("Name this lake from its shape.")).toBeInTheDocument();
    expect(screen.getByText("Shape only. The scale bar shows its size.")).toBeInTheDocument();
  });

  it("offers three hints in order, each with text", async () => {
    const user = userEvent.setup();
    await renderLake();

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    expect(screen.getByText("Continent: Asia")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    expect(screen.getByText("Lies in: Russia")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));
    expect(screen.getByText("Starts with “B” (after “Lake”)")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });

  it("says a lake shared by several countries has shores in them", async () => {
    const user = userEvent.setup();
    await renderLake({ lake: { ...makeLakeData(), countries: ["Kenya", "Tanzania", "Uganda"] } });

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));

    expect(screen.getByText("Shores in: Kenya, Tanzania, Uganda")).toBeInTheDocument();
  });

  it.each(["Lake Baikal", "baikal", "  BAIKAL "])("accepts %j", async (text) => {
    const { submitAnswer } = await renderLake();

    await userEvent.setup().type(input(), text + "{Enter}");

    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "CORRECT", hintsUsed: 0 });
  });

  it("rejects another lake and charges a guess", async () => {
    const { submitAnswer } = await renderLake();

    await userEvent.setup().type(input(), "Lake Victoria{Enter}");

    expect(screen.getByText(`"Lake Victoria" isn't right. 4 guesses left.`)).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
  });

  it("does not charge a guess for an unknown name", async () => {
    const { submitAnswer } = await renderLake();

    await userEvent.setup().type(input(), "Bakal{Enter}");

    expect(screen.getByText(`We don't recognise "Bakal" as a lake. Check the spelling.`)).toBeInTheDocument();
    expect(screen.getByRole("status", { name: "5 guesses left" })).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
  });

  it("suggests the lakes in the game", async () => {
    await renderLake();

    const options = Array.from(document.querySelectorAll("datalist option")).map((o) => o.getAttribute("value"));
    expect(options).toEqual(["Lake Baikal", "Lake Victoria"]);
  });

  it("shows the satellite map once the round is over", async () => {
    await renderLake();
    expect(screen.getByTestId("map")).toHaveAttribute("data-game-over", "false");

    await userEvent.setup().type(input(), "Baikal{Enter}");

    expect(screen.getByTestId("map")).toHaveAttribute("data-game-over", "true");
  });
});
