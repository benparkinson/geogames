import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { makeRoundProps } from "../../test/fixtures";
import MapGame, { HintConfig } from "./MapGame";

const ANSWERS = [
  { name: "France", additionalNames: [] },
  { name: "Spain", additionalNames: [] },
];

function MapStub({ gameOver, gaveUp }: { data: unknown; gameOver: boolean; gaveUp: boolean }) {
  return <div data-testid="map" data-game-over={String(gameOver)} data-gave-up={String(gaveUp)} />;
}

function renderGame(overrides: Partial<React.ComponentProps<typeof MapGame<object>>> = {}) {
  const submitAnswer = vi.fn();
  const props: React.ComponentProps<typeof MapGame<object>> = {
    data: {},
    answerCount: 2,
    MapComponent: MapStub,
    answers: ANSWERS,
    round: makeRoundProps(),
    prompt: "Name them",
    inputPlaceholder: "Country…",
    answerNoun: "country",
    submitAnswer,
    ...overrides,
  };
  render(<MapGame {...props} />);
  return { props, submitAnswer: props.submitAnswer as ReturnType<typeof vi.fn> };
}

const input = () => screen.getByRole("textbox", { name: "Country…" });
const map = () => screen.getByTestId("map");

async function guess(user: ReturnType<typeof userEvent.setup>, text: string) {
  await user.type(input(), text + "{Enter}");
}

async function reveal(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole("button", { name: /Reveal answer/ }));
  await user.click(screen.getByRole("button", { name: /Tap again to reveal/ }));
}

describe("MapGame fresh round", () => {
  it("starts with nothing found and the map still hiding the answer", () => {
    renderGame({ blankMap: true, mapCaption: "Shape only." });

    expect(screen.getByText("Name them")).toBeInTheDocument();
    expect(screen.getByRole("group", { name: "Answers: 0 of 2 found" })).toBeInTheDocument();
    expect(map()).toHaveAttribute("data-game-over", "false");
    expect(map()).toHaveAttribute("data-gave-up", "false");
    expect(screen.getByText("Shape only.")).toBeInTheDocument();
  });

  it("shows no caption when none is configured", () => {
    renderGame();

    expect(screen.queryByText("Shape only.")).not.toBeInTheDocument();
  });

  it("can show a caption on a normal map", () => {
    renderGame({ mapCaption: "Zoom is limited." });

    expect(screen.getByText("Zoom is limited.")).toBeInTheDocument();
    expect(document.querySelector(".game-map--blank")).toBeNull();
  });

  it("can blank the map without a caption", () => {
    renderGame({ blankMap: true });

    expect(document.querySelector(".game-map--blank")).not.toBeNull();
    expect(document.querySelector(".map-caption")).toBeNull();
  });
});

describe("MapGame guessing", () => {
  it("lists a wrong guess without reporting an answer", async () => {
    const { submitAnswer } = renderGame();

    await guess(userEvent.setup(), "Italy");

    expect(screen.getByText('"Italy" isn\'t right. 4 guesses left.')).toBeInTheDocument();
    expect(screen.getByText("Wrong guesses")).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
  });

  it("accepts a correct guess and counts what is left", async () => {
    const { submitAnswer } = renderGame();

    await guess(userEvent.setup(), "france");

    expect(screen.getByText("Correct! 1 to go.")).toBeInTheDocument();
    expect(screen.getByRole("group", { name: "Answers: 1 of 2 found" })).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
  });

  it("does not count a repeated guess twice, whatever its case or accents", async () => {
    const user = userEvent.setup();
    renderGame();

    await guess(user, "France");
    await guess(user, "FRANCE");
    expect(screen.getByText("You've already guessed FRANCE.")).toBeInTheDocument();

    await user.clear(input());
    await guess(user, "Fráncé");
    expect(screen.getByText("You've already guessed Fráncé.")).toBeInTheDocument();
    expect(screen.getByRole("group", { name: "Answers: 1 of 2 found" })).toBeInTheDocument();
  });

  it("rejects an unknown name without recording it as a wrong guess", async () => {
    renderGame({ isKnownGuess: () => false });

    await guess(userEvent.setup(), "Atlantis");

    expect(screen.getByText(/We don't recognise "Atlantis"/)).toBeInTheDocument();
    expect(screen.queryByText("Wrong guesses")).not.toBeInTheDocument();
  });

  it("asks for a guess when the input is blank", async () => {
    renderGame();

    await userEvent.setup().type(input(), "   {Enter}");

    expect(screen.getByText("Type a guess first.")).toBeInTheDocument();
  });

  it("finishes the round exactly once when every answer is found", async () => {
    const user = userEvent.setup();
    const { submitAnswer } = renderGame({ blankMap: true, mapCaption: "Shape only." });

    await guess(user, "France");
    await guess(user, "Spain");

    expect(submitAnswer).toHaveBeenCalledTimes(1);
    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "CORRECT", hintsUsed: 0 });
    expect(map()).toHaveAttribute("data-game-over", "true");
    expect(map()).toHaveAttribute("data-gave-up", "false");
    expect(screen.getByRole("button", { name: /Next round/ })).toBeInTheDocument();
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
    expect(screen.queryByText("Shape only.")).not.toBeInTheDocument();
  });

  it("ignores guesses after the round is over", async () => {
    renderGame({ round: makeRoundProps({ answerState: "CORRECT" }) });

    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
  });
});

describe("MapGame giving up", () => {
  it("reports GAVE_UP once and reveals the answers that weren't found", async () => {
    const { submitAnswer } = renderGame();

    await reveal(userEvent.setup());

    expect(submitAnswer).toHaveBeenCalledTimes(1);
    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "GAVE_UP", hintsUsed: 0 });
    expect(screen.getByText("Answer revealed · 0 pts")).toBeInTheDocument();
    expect(map()).toHaveAttribute("data-game-over", "true");
    expect(map()).toHaveAttribute("data-gave-up", "true");
    expect(screen.getByRole("group", { name: "Answers: 0 of 2 found" })).toBeInTheDocument();
    expect(screen.getByText("France")).toBeInTheDocument();
    expect(screen.getByText("Spain")).toBeInTheDocument();
    expect(screen.getAllByText("(revealed)")).toHaveLength(2);
  });

  it("keeps a found answer and the wrong guesses, and reveals only the rest", async () => {
    const user = userEvent.setup();
    renderGame();

    await guess(user, "Spain");
    await guess(user, "Italy");
    await reveal(user);

    expect(screen.getByRole("group", { name: "Answers: 1 of 2 found" })).toBeInTheDocument();
    expect(screen.getAllByText("(revealed)")).toHaveLength(1);
    expect(screen.getByText("France")).toBeInTheDocument();
    expect(screen.getByText("Italy")).toBeInTheDocument();
  });
});

describe("MapGame resuming a finished round", () => {
  it("starts solved with every answer filled in when the round was answered correctly", () => {
    const { submitAnswer } = renderGame({ round: makeRoundProps({ answerState: "CORRECT" }) });

    expect(map()).toHaveAttribute("data-game-over", "true");
    expect(map()).toHaveAttribute("data-gave-up", "false");
    expect(screen.getByRole("group", { name: "Answers: 2 of 2 found" })).toBeInTheDocument();
    expect(screen.getByText("Perfect! +100")).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
  });

  it("shows the points and hints of an answered round, and its hint texts", () => {
    const { submitAnswer } = renderGame({
      round: makeRoundProps({ answerState: "CORRECT", hintCount: 3, hintsUsed: 2 }),
      hints: { description: "A clue", text: (index) => `Hint number ${index}` },
    });

    expect(screen.getByText("Correct! +67")).toBeInTheDocument();
    expect(screen.getByText("2 hints used")).toBeInTheDocument();
    expect(screen.getByText("Hint number 0")).toBeInTheDocument();
    expect(screen.getByText("Hint number 1")).toBeInTheDocument();
    expect(screen.queryByText("Hint number 2")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Hint/ })).not.toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
  });

  it("starts revealed when the round was given up", () => {
    const { submitAnswer } = renderGame({ round: makeRoundProps({ answerState: "GAVE_UP" }) });

    expect(map()).toHaveAttribute("data-game-over", "true");
    expect(map()).toHaveAttribute("data-gave-up", "true");
    expect(screen.getByText("Answer revealed · 0 pts")).toBeInTheDocument();
    expect(screen.getAllByText("(revealed)")).toHaveLength(2);
    expect(submitAnswer).not.toHaveBeenCalled();
  });
});

describe("MapGame hints", () => {
  function hintConfig() {
    const text = vi.fn<(hintIndex: number) => string | null>((index) => (index === 0 ? "Continent: Europe" : null));
    const hints: HintConfig = { description: "A clue", text };
    return { hints, text };
  }

  it("calls the hint with an increasing index and shows the text it returns", async () => {
    const user = userEvent.setup();
    const { hints, text } = hintConfig();
    renderGame({ hints, round: makeRoundProps({ hintCount: 2 }) });

    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    expect(text).toHaveBeenLastCalledWith(0);
    expect(screen.getByText("Continent: Europe")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));
    expect(text).toHaveBeenLastCalledWith(1);
  });

  it("uses up a hint even when it has no text, and stops when none are left", async () => {
    const user = userEvent.setup();
    const { hints } = hintConfig();
    renderGame({ hints, round: makeRoundProps({ hintCount: 2 }) });

    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));

    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
    expect(screen.getAllByText(/Continent/)).toHaveLength(1);
  });

  it("takes a hint that has no text function without showing anything", async () => {
    const user = userEvent.setup();
    renderGame({ hints: { description: "Changes the map" }, round: makeRoundProps({ hintCount: 1 }) });

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));

    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });

  it("tells the map how many hints have been taken", async () => {
    const user = userEvent.setup();
    const seen: number[] = [];
    renderGame({
      hints: { description: "A clue" },
      round: makeRoundProps({ hintCount: 2 }),
      MapComponent: ({ hintsUsed }) => {
        seen.push(hintsUsed);
        return <div data-testid="map" />;
      },
    });

    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));

    expect(seen[0]).toBe(0);
    expect(seen[seen.length - 1]).toBe(1);
  });

  it("reports the hints used with the answer", async () => {
    const user = userEvent.setup();
    const { submitAnswer } = renderGame({
      hints: { description: "A clue" },
      round: makeRoundProps({ hintCount: 3 }),
    });

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    await guess(user, "France");
    await guess(user, "Spain");

    expect(submitAnswer).toHaveBeenCalledTimes(1);
    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "CORRECT", hintsUsed: 1 });
  });

  it("reports the hints used when the player gives up", async () => {
    const user = userEvent.setup();
    const { submitAnswer } = renderGame({
      hints: { description: "A clue" },
      round: makeRoundProps({ hintCount: 3 }),
    });

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    await reveal(user);

    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "GAVE_UP", hintsUsed: 2 });
  });

  it("shows what the round is worth, 100 then 83, 67 and 50 for three hints", async () => {
    const user = userEvent.setup();
    renderGame({ hints: { description: "A clue" }, round: makeRoundProps({ hintCount: 3 }) });
    expect(screen.getByText("Worth 100 pts")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    expect(screen.getByText("Worth 83 pts")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    expect(screen.getByText("Worth 67 pts")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));
    expect(screen.getByText("Worth 50 pts")).toBeInTheDocument();
  });

  it("shows what the round is worth, 100 then 50, for one hint", async () => {
    const user = userEvent.setup();
    renderGame({ hints: { description: "A clue" }, round: makeRoundProps({ hintCount: 1 }) });
    expect(screen.getByText("Worth 100 pts")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));

    expect(screen.getByText("Worth 50 pts")).toBeInTheDocument();
  });

  it("offers no hints when the backend says there are none", () => {
    renderGame({ hints: { description: "A clue" }, round: makeRoundProps({ hintCount: 0 }) });

    expect(screen.queryByRole("button", { name: /Hint/ })).not.toBeInTheDocument();
    expect(screen.getByText("Worth 100 pts")).toBeInTheDocument();
  });

  it("offers no hint button when a game forgets to supply its hints", () => {
    renderGame({ hints: undefined, round: makeRoundProps({ hintCount: 2 }) });

    expect(screen.queryByRole("button", { name: /Hint/ })).not.toBeInTheDocument();
  });
});

describe("MapGame guess limit", () => {
  const limited = () => renderGame({ round: makeRoundProps({ guessLimit: 2 }) });

  it("ends the round on the last wrong guess and reveals what wasn't found", async () => {
    const user = userEvent.setup();
    const { submitAnswer } = limited();

    await guess(user, "Spain");
    await guess(user, "Italy");
    expect(screen.getByRole("status", { name: "1 guess left" })).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
    await guess(user, "Peru");

    expect(submitAnswer).toHaveBeenCalledTimes(1);
    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "OUT_OF_GUESSES", hintsUsed: 0 });
    expect(screen.getByText("Out of guesses · 0 pts")).toBeInTheDocument();
    expect(map()).toHaveAttribute("data-game-over", "true");
    expect(map()).toHaveAttribute("data-gave-up", "true");
    expect(screen.getByRole("group", { name: "Answers: 1 of 2 found" })).toBeInTheDocument();
    expect(screen.getAllByText("(revealed)")).toHaveLength(1);
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("doesn't count a correct guess between wrong ones", async () => {
    const user = userEvent.setup();
    const { submitAnswer } = limited();

    await guess(user, "Italy");
    await guess(user, "Spain");

    expect(screen.getByRole("status", { name: "1 guess left" })).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
  });

  it("starts a reopened OUT_OF_GUESSES round as revealed", () => {
    const { submitAnswer } = renderGame({ round: makeRoundProps({ answerState: "OUT_OF_GUESSES" }) });

    expect(screen.getByText("Out of guesses · 0 pts")).toBeInTheDocument();
    expect(map()).toHaveAttribute("data-gave-up", "true");
    expect(submitAnswer).not.toHaveBeenCalled();
  });
});
