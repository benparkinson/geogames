import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { mockFetch } from "../../test/fetchMock";
import { makeRiverRoundData, makeRoundProps, riverNames } from "../../test/fixtures";
import { renderWithClient } from "../../test/render";
import { makeRiverGame } from "./RiverGame";
import { continentHint, textHints } from "./riverHints";

const CAPTION = "Shape only. The satellite map appears when you answer.";

const RiverGame = makeRiverGame({
  MapComponent: ({ gameOver, gaveUp }: { gameOver: boolean; gaveUp: boolean }) => (
    <div data-testid="map" data-game-over={String(gameOver)} data-gave-up={String(gaveUp)} />
  ),
  prompt: "Name this river from its shape.",
  blankMap: true,
  mapCaption: CAPTION,
  hints: (river) => textHints(river, continentHint),
});

/** Renders the game and, unless the list is meant to fail, waits for the river names to load. */
async function renderRiver({ listFails = false } = {}) {
  const submitAnswer = vi.fn();
  mockFetch([{ url: /\/api\/rivers$/, ...(listFails ? { status: 503, body: {} } : { body: riverNames }) }]);
  const { client } = renderWithClient(<RiverGame data={makeRiverRoundData()} round={makeRoundProps({ hintCount: 1 })} answerCount={1} submitAnswer={submitAnswer} />);
  if (!listFails) {
    await waitFor(() => expect(client.getQueryData(["placeNames", "rivers"])).toBeDefined());
  }
  return { submitAnswer };
}

const input = () => screen.getByLabelText("River name…");

describe("makeRiverGame with the shape configuration", () => {
  it("asks for the river's name", async () => {
    await renderRiver();

    expect(screen.getByText("Name this river from its shape.")).toBeInTheDocument();
  });

  it.each(["Danube", "danube", "  DANUBE "])("accepts %j", async (text) => {
    const { submitAnswer } = await renderRiver();

    await userEvent.setup().type(input(), text + "{Enter}");

    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "CORRECT", hintsUsed: 0 });
  });

  it("accepts an alias", async () => {
    const { submitAnswer } = await renderRiver();

    await userEvent.setup().type(input(), "Donau{Enter}");

    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "CORRECT", hintsUsed: 0 });
  });

  it("rejects another river", async () => {
    const { submitAnswer } = await renderRiver();

    await userEvent.setup().type(input(), "Thames{Enter}");

    expect(screen.getByText('"Thames" isn\'t right. 4 guesses left.')).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
  });

  it("suggests the rivers in the game, by display name", async () => {
    await renderRiver();

    const options = Array.from(document.querySelectorAll("datalist option")).map((o) => o.getAttribute("value"));
    expect(options).toEqual(["Danube", "Thames"]);
  });

  it("does not charge a guess for an unknown name", async () => {
    const { submitAnswer } = await renderRiver();

    await userEvent.setup().type(input(), "Danub{Enter}");

    expect(screen.getByText(`We don't recognise "Danub" as a river. Check the spelling.`)).toBeInTheDocument();
    expect(screen.getByRole("status", { name: "5 guesses left" })).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
  });

  it("charges a guess for a known but wrong river", async () => {
    await renderRiver();

    await userEvent.setup().type(input(), "Thames{Enter}");

    expect(screen.getByText(`"Thames" isn't right. 4 guesses left.`)).toBeInTheDocument();
  });

  it("counts a misspelling as wrong when the river list fails to load", async () => {
    await renderRiver({ listFails: true });

    await userEvent.setup().type(input(), "Danub{Enter}");

    expect(screen.getByText(`"Danub" isn't right. 4 guesses left.`)).toBeInTheDocument();
  });

  it("offers a single hint that names the continent", async () => {
    const user = userEvent.setup();
    await renderRiver();

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));

    expect(screen.getByText("Continent: Europe")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });

  it("hides the satellite map until the round ends", async () => {
    await renderRiver();
    expect(screen.getByText(CAPTION)).toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-game-over", "false");

    await userEvent.setup().type(input(), "Danube{Enter}");

    expect(screen.queryByText(CAPTION)).not.toBeInTheDocument();
    expect(screen.getByTestId("map")).toHaveAttribute("data-game-over", "true");
  });
});
