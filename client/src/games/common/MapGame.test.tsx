import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { makeRoundProps } from "../../test/fixtures";
import MapGame from "./MapGame";
import type { HintConfig } from "./hints";

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
    hints: { description: "A clue" },
    submitAnswer,
    ...overrides,
  };
  render(<MapGame {...props} />);
  return { props, submitAnswer: props.submitAnswer as ReturnType<typeof vi.fn> };
}

const input = () => screen.getByRole("textbox", { name: "Country…" });
const map = () => screen.getByTestId("map");

/** The round's result banner, the live status that carries the outcome, reads `text` (label, note and points). */
function expectResult(text: string): void {
  const banner = screen.getAllByRole("status").find((element) => element.hasAttribute("data-outcome"));
  expect(banner?.textContent?.replace(/\s+/g, " ").trim()).toBe(text);
}

async function guess(user: ReturnType<typeof userEvent.setup>, text: string) {
  await user.type(input(), text + "{Enter}");
}

async function reveal(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole("button", { name: /^Give up$/ }));
  await user.click(screen.getByRole("button", { name: /Tap again to give up/ }));
}

describe("MapGame fresh round", () => {
  it("starts with nothing found and the map still hiding the answer", () => {
    renderGame({ blankMap: true });

    expect(screen.getByText("Name them")).toBeInTheDocument();
    expect(screen.getByRole("group", { name: "Answers: 0 of 2 found" })).toBeInTheDocument();
    expect(map()).toHaveAttribute("data-game-over", "false");
    expect(map()).toHaveAttribute("data-gave-up", "false");
  });

  it("shows the round name, and the question in that order, before the answer slots", () => {
    renderGame();

    const name = screen.getAllByText("Test Game").at(-1) as HTMLElement;
    const question = screen.getByRole("heading", { level: 1, name: "Name them" });
    const slots = screen.getByRole("group", { name: /Answers:/ });
    expect(name.compareDocumentPosition(question) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(question.compareDocumentPosition(slots) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it("puts nothing on the map but the map", () => {
    renderGame();

    expect(map().parentElement?.children).toHaveLength(1);
  });

  it("can blank the map by itself", () => {
    renderGame({ blankMap: true });

    expect(screen.getByRole("main")).toHaveAttribute("data-blank-map");
  });
});

describe("MapGame guessing", () => {
  it("lists a wrong guess without reporting an answer", async () => {
    const { submitAnswer } = renderGame();

    await guess(userEvent.setup(), "Italy");

    expect(screen.getByText("Italy")).toBeInTheDocument();
    expect(screen.getByTestId("wrong-guess")).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
  });

  it("accepts a correct guess and counts what is left", async () => {
    const { submitAnswer } = renderGame();

    await guess(userEvent.setup(), "france");

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
    expect(screen.queryByTestId("wrong-guess")).not.toBeInTheDocument();
  });

  it("asks for a guess when the input is blank", async () => {
    renderGame();

    await userEvent.setup().type(input(), "   {Enter}");

    expect(screen.getByText("Type a guess first.")).toBeInTheDocument();
  });

  it("finishes the round exactly once when every answer is found", async () => {
    const user = userEvent.setup();
    const { submitAnswer } = renderGame({ blankMap: true });

    await guess(user, "France");
    await guess(user, "Spain");

    expect(submitAnswer).toHaveBeenCalledTimes(1);
    expect(submitAnswer).toHaveBeenCalledWith({
      answerState: "CORRECT",
      hintsUsed: 0,
      wrongGuesses: 0,
      answersFound: 2,
    });
    expect(map()).toHaveAttribute("data-game-over", "true");
    expect(map()).toHaveAttribute("data-gave-up", "false");
    expect(screen.getByRole("button", { name: /Next round/ })).toBeInTheDocument();
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
    expect(screen.getByText("Name them")).toBeInTheDocument();
    expect(screen.getAllByText("Test Game").length).toBeGreaterThan(0);
  });

  it("reports only the guesses that count against the limit as wrong guesses", async () => {
    const user = userEvent.setup();
    const { submitAnswer } = renderGame({ isKnownGuess: (text) => text !== "Atlantis" });

    await guess(user, "Italy"); // wrong: counts
    await user.clear(input());
    await guess(user, "Italy"); // duplicate: doesn't count
    await user.clear(input());
    await guess(user, "Atlantis"); // unknown: doesn't count
    await user.clear(input());
    await guess(user, "France"); // right: doesn't count
    await user.clear(input());
    await guess(user, "Spain");

    expect(submitAnswer).toHaveBeenCalledTimes(1);
    expect(submitAnswer).toHaveBeenCalledWith({
      answerState: "CORRECT",
      hintsUsed: 0,
      wrongGuesses: 1,
      answersFound: 2,
    });
  });

  it("ignores guesses after the round is over", async () => {
    renderGame({ round: makeRoundProps({ answerState: "CORRECT" }) });

    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
  });
});

describe("MapGame giving up", () => {
  it("reports how many answers were found, and scores their share, when giving up part way", async () => {
    const user = userEvent.setup();
    const { submitAnswer } = renderGame();

    await guess(user, "France");
    await reveal(user);

    expect(submitAnswer).toHaveBeenCalledWith({
      answerState: "GAVE_UP",
      hintsUsed: 0,
      wrongGuesses: 0,
      answersFound: 1,
    });
    expectResult("Answer revealed 1 of 2 found +50");
  });

  it("reports the wrong guesses made before giving up", async () => {
    const user = userEvent.setup();
    const { submitAnswer } = renderGame();

    await guess(user, "Italy");
    await user.clear(input());
    await guess(user, "Peru");
    await reveal(user);

    expect(submitAnswer).toHaveBeenCalledWith({
      answerState: "GAVE_UP",
      hintsUsed: 0,
      wrongGuesses: 2,
      answersFound: 0,
    });
  });

  it("reports GAVE_UP once and reveals the answers that weren't found", async () => {
    const { submitAnswer } = renderGame();

    await reveal(userEvent.setup());

    expect(submitAnswer).toHaveBeenCalledTimes(1);
    expect(submitAnswer).toHaveBeenCalledWith({
      answerState: "GAVE_UP",
      hintsUsed: 0,
      wrongGuesses: 0,
      answersFound: 0,
    });
    expectResult("Answer revealed +0");
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
    expectResult("No wrong guesses! +100");
    expect(submitAnswer).not.toHaveBeenCalled();
  });

  it("shows the points and hints of an answered round, and its hint texts", () => {
    const { submitAnswer } = renderGame({
      round: makeRoundProps({ answerState: "CORRECT", hintCount: 3, hintsUsed: 2 }),
      hints: { description: "A clue", text: (index) => `Hint number ${index}` },
    });

    expectResult("Correct with 2 hints +67");
    expect(screen.getByText("Hint number 0").closest("ul")).toBe(screen.getByRole("list", { name: "Hints" }));
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
    expectResult("Answer revealed +0");
    expect(screen.getAllByText("(revealed)")).toHaveLength(2);
    expect(submitAnswer).not.toHaveBeenCalled();
  });
});

describe("MapGame continent line", () => {
  it("shows the continent from the start of an Easy round, before any hint", () => {
    renderGame({ round: makeRoundProps({ difficulty: "EASY", continents: ["Europe"] }) });

    const line = screen.getByText("Continent: Europe");
    expect(line.closest("ul")).toBe(screen.getByRole("list", { name: "Hints" }));
    expect(line.querySelector("svg")).toHaveAttribute("aria-hidden", "true");
  });

  it("names both continents of a country that spans two", () => {
    renderGame({ round: makeRoundProps({ difficulty: "EASY", continents: ["Asia", "Europe"] }) });

    expect(screen.getByText("Continents: Asia / Europe")).toBeInTheDocument();
  });

  it("shows nothing in a Normal round", () => {
    renderGame({ round: makeRoundProps({ difficulty: "NORMAL", continents: [] }) });

    expect(screen.queryByText(/^Continents?:/)).not.toBeInTheDocument();
    expect(screen.queryByRole("list", { name: "Hints" })).not.toBeInTheDocument();
  });

  it("is the first item of the list, above the hints taken", async () => {
    const user = userEvent.setup();
    renderGame({
      round: makeRoundProps({ difficulty: "EASY", continents: ["Africa"], hintCount: 1 }),
      hints: { description: "A clue", text: () => "First letter: K" },
    });

    await user.click(screen.getByRole("button", { name: /Hint/ }));

    const items = within(screen.getByRole("list", { name: "Hints" }))
      .getAllByRole("listitem")
      .map((li) => li.textContent?.trim());
    expect(items).toEqual(["Continent: Africa", "First letter: K"]);
  });

  it("keeps showing the continent once the round is over", () => {
    renderGame({ round: makeRoundProps({ difficulty: "EASY", continents: ["Oceania"], answerState: "GAVE_UP" }) });

    expect(screen.getByText("Continent: Oceania")).toBeInTheDocument();
  });
});

describe("MapGame hint button", () => {
  it("offers no hint button once the round is over", async () => {
    renderGame({
      hints: { description: "A clue" },
      round: makeRoundProps({ hintCount: 2, answerState: "GAVE_UP" }),
    });

    expect(screen.queryByRole("button", { name: /Hint/ })).not.toBeInTheDocument();
  });
});

describe("MapGame hints", () => {
  function hintConfig() {
    const text = vi.fn<(hintIndex: number, foundAtHint: string[][]) => string | null>((index) =>
      index === 0 ? "Continent: Europe" : null,
    );
    const hints: HintConfig = { description: "A clue", text };
    return { hints, text };
  }

  it("calls the hint with an increasing index and shows the text it returns", async () => {
    const user = userEvent.setup();
    const { hints, text } = hintConfig();
    renderGame({ hints, round: makeRoundProps({ hintCount: 2 }) });

    await user.click(screen.getByRole("button", { name: /Hint .*· 2 left/ }));
    expect(text).toHaveBeenLastCalledWith(0, [[]]);
    expect(screen.getByText("Continent: Europe")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint .*· 1 left/ }));
    expect(text).toHaveBeenLastCalledWith(1, [[], []]);
  });

  it("uses up a hint even when it has no text, and stops when none are left", async () => {
    const user = userEvent.setup();
    const { hints } = hintConfig();
    renderGame({ hints, round: makeRoundProps({ hintCount: 2 }) });

    await user.click(screen.getByRole("button", { name: /Hint .*· 2 left/ }));
    await user.click(screen.getByRole("button", { name: /Hint .*· 1 left/ }));

    expect(screen.getByRole("button", { name: /Hint · 0 left/ })).toBeDisabled();
    expect(screen.getAllByText(/Continent/)).toHaveLength(1);
  });

  it("takes a hint that has no text function without showing anything", async () => {
    const user = userEvent.setup();
    renderGame({ hints: { description: "Changes the map" }, round: makeRoundProps({ hintCount: 1 }) });

    await user.click(screen.getByRole("button", { name: /Hint .*· 1 left/ }));

    expect(screen.getByRole("button", { name: /Hint · 0 left/ })).toBeDisabled();
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

    await user.click(screen.getByRole("button", { name: /Hint .*· 2 left/ }));

    expect(seen[0]).toBe(0);
    expect(seen[seen.length - 1]).toBe(1);
  });

  it("tells the map which answers were found when each hint was taken", async () => {
    const user = userEvent.setup();
    const seen: (string[][] | undefined)[] = [];
    renderGame({
      hints: { description: "A clue" },
      round: makeRoundProps({ hintCount: 2 }),
      MapComponent: ({ foundAtHint }) => {
        seen.push(foundAtHint);
        return <div data-testid="map" />;
      },
    });

    await guess(user, "France");
    await user.click(screen.getByRole("button", { name: /Hint .*· 2 left/ }));

    expect(seen[seen.length - 1]).toEqual([["France"]]);
  });

  it("reports the hints used with the answer", async () => {
    const user = userEvent.setup();
    const { submitAnswer } = renderGame({
      hints: { description: "A clue" },
      round: makeRoundProps({ hintCount: 3 }),
    });

    await user.click(screen.getByRole("button", { name: /Hint .*· 3 left/ }));
    await guess(user, "France");
    await guess(user, "Spain");

    expect(submitAnswer).toHaveBeenCalledTimes(1);
    expect(submitAnswer).toHaveBeenCalledWith({
      answerState: "CORRECT",
      hintsUsed: 1,
      wrongGuesses: 0,
      answersFound: 2,
    });
  });

  it("reports the hints used when the player gives up", async () => {
    const user = userEvent.setup();
    const { submitAnswer } = renderGame({
      hints: { description: "A clue" },
      round: makeRoundProps({ hintCount: 3 }),
    });

    await user.click(screen.getByRole("button", { name: /Hint .*· 3 left/ }));
    await user.click(screen.getByRole("button", { name: /Hint .*· 2 left/ }));
    await reveal(user);

    expect(submitAnswer).toHaveBeenCalledWith({
      answerState: "GAVE_UP",
      hintsUsed: 2,
      wrongGuesses: 0,
      answersFound: 0,
    });
  });

  it("shows what the round is worth, 100 then 83, 67 and 50 for three hints", async () => {
    const user = userEvent.setup();
    renderGame({ hints: { description: "A clue" }, round: makeRoundProps({ hintCount: 3 }) });
    expect(screen.getByText("Worth 100")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint .*· 3 left/ }));
    expect(screen.getByText("Worth 83")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Hint .*· 2 left/ }));
    expect(screen.getByText("Worth 67")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Hint .*· 1 left/ }));
    expect(screen.getByText("Worth 50")).toBeInTheDocument();
  });

  it("shows what the round is worth, 100 then 50, for one hint", async () => {
    const user = userEvent.setup();
    renderGame({ hints: { description: "A clue" }, round: makeRoundProps({ hintCount: 1 }) });
    expect(screen.getByText("Worth 100")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint .*· 1 left/ }));

    expect(screen.getByText("Worth 50")).toBeInTheDocument();
  });
});

describe("MapGame panel", () => {
  const saved = (text: string) => ({ text, correct: false });

  it("shows the lives left before any guess", () => {
    renderGame();

    expect(screen.getByRole("status", { name: "5 lives left" })).toBeInTheDocument();
  });

  it("shows the lives left and the value together in the status row, not in the button row", () => {
    renderGame({ round: makeRoundProps({ guesses: [saved("Italy"), saved("Peru")] }) });

    const row = screen.getByTestId("round-status");
    expect(row).toContainElement(screen.getByRole("status", { name: "3 lives left" }));
    expect(row).toHaveTextContent(/Worth \d+$/);
    const reveal = screen.getByRole("button", { name: /^Give up$/ });
    expect(reveal.parentElement).not.toHaveTextContent("Worth");
  });

  it("puts the status row between the guess list and the input, beside the guess", () => {
    renderGame();

    const row = screen.getByTestId("round-status");
    const list = screen.getByRole("group", { name: /^Answers:/ });
    expect(list.compareDocumentPosition(row) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(row.compareDocumentPosition(input()) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it("says when it was the next to last guess", async () => {
    renderGame({ round: makeRoundProps({ guessLimit: 2 }) });

    await guess(userEvent.setup(), "Italy");

    expect(screen.getByText("Italy")).toBeInTheDocument();
    expect(screen.getByText("Last life!")).toBeInTheDocument();
  });

  it("shows no wrong guesses heading when there are none", () => {
    renderGame();

    expect(screen.queryByTestId("wrong-guess")).not.toBeInTheDocument();
  });

  it("has no status row or feedback line once the round is over", () => {
    renderGame({ round: makeRoundProps({ answerState: "CORRECT" }) });

    expect(screen.queryByTestId("round-status")).toBeNull();
    expect(screen.queryByTestId("guess-feedback")).toBeNull();
  });

  it("shows no feedback once the round is over", async () => {
    const user = userEvent.setup();
    renderGame();

    await guess(user, "France");
    await guess(user, "Spain");

    expect(screen.queryByTestId("guess-feedback")).toBeNull();
  });

  it("shows the round result instead of the form when the round is over", () => {
    renderGame({ round: makeRoundProps({ answerState: "CORRECT" }) });

    expectResult("No wrong guesses! +100");
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Guess" })).not.toBeInTheDocument();
  });

  it("shows out of guesses instead of the form", () => {
    renderGame({ round: makeRoundProps({ answerState: "OUT_OF_GUESSES" }) });

    expectResult("Out of guesses +0");
    expect(screen.queryByText("Answer revealed")).not.toBeInTheDocument();
    expect(screen.queryByRole("status", { name: /lives left/ })).not.toBeInTheDocument();
  });
});

describe("MapGame guess limit", () => {
  const limited = () => renderGame({ round: makeRoundProps({ guessLimit: 2 }) });

  it("ends the round on the last wrong guess and reveals what wasn't found", async () => {
    const user = userEvent.setup();
    const { submitAnswer } = limited();

    await guess(user, "Spain");
    await guess(user, "Italy");
    expect(screen.getByRole("status", { name: "1 life left" })).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
    await guess(user, "Peru");

    expect(submitAnswer).toHaveBeenCalledTimes(1);
    expect(submitAnswer).toHaveBeenCalledWith({
      answerState: "OUT_OF_GUESSES",
      hintsUsed: 0,
      wrongGuesses: 2,
      answersFound: 1,
    });
    expectResult("Out of guesses 1 of 2 found +50");
    expect(map()).toHaveAttribute("data-game-over", "true");
    expect(map()).toHaveAttribute("data-gave-up", "true");
    expect(screen.getByRole("group", { name: "Answers: 1 of 2 found" })).toBeInTheDocument();
    expect(screen.getAllByText("(revealed)")).toHaveLength(1);
    expect(screen.queryByRole("status", { name: /lives left/ })).not.toBeInTheDocument();
  });

  it("doesn't count a correct guess between wrong ones", async () => {
    const user = userEvent.setup();
    const { submitAnswer } = limited();

    await guess(user, "Italy");
    await guess(user, "Spain");

    expect(screen.getByRole("status", { name: "1 life left" })).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
  });

  it("starts a reopened OUT_OF_GUESSES round as revealed", () => {
    const { submitAnswer } = renderGame({ round: makeRoundProps({ answerState: "OUT_OF_GUESSES" }) });

    expectResult("Out of guesses +0");
    expect(map()).toHaveAttribute("data-gave-up", "true");
    expect(submitAnswer).not.toHaveBeenCalled();
  });
});

describe("MapGame clues", () => {
  const clueFor = vi.fn(() => ({ km: 1236, bearing: 45 }));

  it("shows the clue on the chip when the round is not in Hard mode", async () => {
    const user = userEvent.setup();
    renderGame({ clueFor, round: makeRoundProps({ difficulty: "NORMAL" }) });

    await guess(user, "Italy");

    expect(screen.getByText(/1,240 km$/)).toBeInTheDocument();
  });

  it("shows no difficulty control during a round", async () => {
    const user = userEvent.setup();
    renderGame({ clueFor });

    await guess(user, "Italy");

    expect(screen.queryByRole("group", { name: "Difficulty" })).not.toBeInTheDocument();
    expect(screen.queryByRole("radio", { name: "On" })).not.toBeInTheDocument();
  });

  it("still shows clues when the preference is on but the round is not in Hard mode", async () => {
    localStorage.setItem("gg-difficulty", "hard");
    const user = userEvent.setup();
    renderGame({ clueFor, round: makeRoundProps({ difficulty: "NORMAL" }) });

    await guess(user, "Italy");

    expect(screen.getByText(/1,240 km$/)).toBeInTheDocument();
  });

  it("shows no clues, and no switch, when the game has no clueFor", async () => {
    const user = userEvent.setup();
    renderGame();

    await guess(user, "Italy");

    expect(screen.queryByRole("radio", { name: "On" })).not.toBeInTheDocument();
    expect(screen.queryByText(/ km /)).not.toBeInTheDocument();
  });

  it("shows no clue when the round is in Hard mode", async () => {
    const user = userEvent.setup();
    renderGame({ clueFor, round: makeRoundProps({ difficulty: "HARD" }) });

    await guess(user, "Italy");

    expect(screen.queryByText(/ km /)).not.toBeInTheDocument();
    expect(screen.getByText("Italy")).toBeInTheDocument();
  });
});

describe("MapGame saving guesses", () => {
  const recordGuess = (props: { round: { recordGuess: unknown } }) =>
    props.round.recordGuess as ReturnType<typeof vi.fn>;

  it("saves a wrong and a correct guess", async () => {
    const user = userEvent.setup();
    const { props } = renderGame();

    await guess(user, "Italy");
    await guess(user, "france");

    expect(recordGuess(props).mock.calls).toEqual([
      [{ text: "Italy", correct: false }],
      [{ text: "france", correct: true }],
    ]);
  });

  it("does not save an empty, duplicate or unknown guess", async () => {
    const user = userEvent.setup();
    const { props } = renderGame({ isKnownGuess: (g) => g !== "Atlantis" });

    await guess(user, "Italy");
    await guess(user, "   ");
    await user.clear(input());
    await guess(user, "ITALY");
    await user.clear(input());
    await guess(user, "Atlantis");

    expect(recordGuess(props)).toHaveBeenCalledTimes(1);
  });

  it("saves the guess that ends the round before it reports the answer", async () => {
    const order: string[] = [];
    const round = makeRoundProps({ recordGuess: vi.fn(() => void order.push("guess")) });
    const submitAnswer = vi.fn(() => void order.push("answer"));
    renderGame({ round, submitAnswer });
    const user = userEvent.setup();

    await guess(user, "France");
    await guess(user, "Spain");

    expect(order).toEqual(["guess", "guess", "answer"]);
  });
});

describe("MapGame resuming a solved round", () => {
  it("measures a late clue to the answer that was still unfound when the guess was made", () => {
    // Played: France, then a wrong Italy, then Spain. Italy was guessed while only Spain was unfound.
    const round = makeRoundProps({
      answerState: "CORRECT",
      guesses: [
        { text: "France", correct: true },
        { text: "Italy", correct: false },
        { text: "Spain", correct: true },
      ],
    });
    const props: React.ComponentProps<typeof MapGame<object>> = {
      data: {},
      answerCount: 2,
      MapComponent: MapStub,
      answers: ANSWERS,
      round,
      prompt: "Name them",

      inputPlaceholder: "Country…",
      answerNoun: "country",
      hints: { description: "A clue" },
      submitAnswer: vi.fn(),
    };
    const { rerender } = render(<MapGame {...props} />);
    expect(screen.queryByText(/ km/)).not.toBeInTheDocument();

    const clueFor = (_guess: string, targets: string[]) => ({
      km: targets.join() === "Spain" ? 480 : 900,
      bearing: 10,
    });
    rerender(<MapGame {...props} clueFor={clueFor} />);

    expect(screen.getAllByText(/480 km/).length).toBeGreaterThan(0);
    expect(screen.queryByText(/900 km/)).not.toBeInTheDocument();
  });
});

describe("MapGame resuming an unfinished round", () => {
  const saved = (text: string, correct = false) => ({ text, correct });

  it("adds the distance clue to a saved guess once the clues are available", () => {
    const round = makeRoundProps({ guesses: [saved("Italy")] });
    const props: React.ComponentProps<typeof MapGame<object>> = {
      data: {},
      answerCount: 2,
      MapComponent: MapStub,
      answers: ANSWERS,
      round,
      prompt: "Name them",

      inputPlaceholder: "Country�",
      answerNoun: "country",
      hints: { description: "A clue" },
      submitAnswer: vi.fn(),
    };
    const { rerender } = render(<MapGame {...props} />);
    expect(screen.queryByText(/ km/)).not.toBeInTheDocument();

    rerender(<MapGame {...props} clueFor={() => ({ km: 480, bearing: 10 })} />);

    expect(screen.getAllByText(/480 km/).length).toBeGreaterThan(0);
  });

  it("shows the saved guesses and the lives left", () => {
    renderGame({
      round: makeRoundProps({ guesses: [saved("Italy"), saved("France", true)] }),
    });

    expect(screen.getByTestId("wrong-guess")).toBeInTheDocument();
    expect(screen.getByText("Italy")).toBeInTheDocument();
    expect(screen.getByRole("group", { name: "Answers: 1 of 2 found" })).toBeInTheDocument();
    expect(screen.getByText(/4 lives left/)).toBeInTheDocument();
  });

  it("rejects a repeat of a saved guess as a duplicate and saves nothing", async () => {
    const { props } = renderGame({ round: makeRoundProps({ guesses: [saved("Italy")] }) });

    await guess(userEvent.setup(), "italy");

    expect(screen.getByText("You've already guessed italy.")).toBeInTheDocument();
    expect(props.round.recordGuess).not.toHaveBeenCalled();
  });

  it("sends the answer once when the saved guesses had already ended the round", () => {
    const round = makeRoundProps({
      guesses: [saved("A"), saved("B"), saved("C"), saved("D"), saved("E")],
    });
    const { submitAnswer } = renderGame({ round });

    expect(submitAnswer).toHaveBeenCalledTimes(1);
    expect(submitAnswer).toHaveBeenCalledWith({
      answerState: "OUT_OF_GUESSES",
      hintsUsed: 0,
      wrongGuesses: 5,
      answersFound: 0,
    });
  });

  it("does not send an answer again for a round the server has answered", () => {
    const round = makeRoundProps({
      answerState: "OUT_OF_GUESSES",
      guesses: [saved("A"), saved("B"), saved("C"), saved("D"), saved("E")],
    });
    const { submitAnswer } = renderGame({ round });

    expect(submitAnswer).not.toHaveBeenCalled();
    expectResult("Out of guesses +0");
  });
});

describe("MapGame saving hints", () => {
  const hints: HintConfig = { description: "A clue", text: (index) => `Clue ${index}` };

  it("saves each hint with its index and the guesses made before it", async () => {
    const user = userEvent.setup();
    const { props } = renderGame({ hints, round: makeRoundProps({ hintCount: 3 }) });

    await user.click(screen.getByRole("button", { name: /Hint .*· 3 left/ }));
    await guess(user, "Italy");
    await user.click(screen.getByRole("button", { name: /Hint .*· 2 left/ }));

    expect((props.round.recordHint as ReturnType<typeof vi.fn>).mock.calls).toEqual([
      [{ hint: 0, guessesBefore: 0 }],
      [{ hint: 1, guessesBefore: 1 }],
    ]);
  });

  it("saves a hint, a guess and the answer in that order", async () => {
    const order: string[] = [];
    const round = makeRoundProps({
      hintCount: 3,
      recordHint: vi.fn(() => void order.push("hint")),
      recordGuess: vi.fn(() => void order.push("guess")),
    });
    const submitAnswer = vi.fn(() => void order.push("answer"));
    renderGame({ hints, round, submitAnswer, answerCount: 1, answers: [ANSWERS[0]] });
    const user = userEvent.setup();

    await user.click(screen.getByRole("button", { name: /Hint .*· 3 left/ }));
    await guess(user, "France");

    expect(order).toEqual(["hint", "guess", "answer"]);
  });

  it("says first guess for a single-answer round won straight away", async () => {
    renderGame({ answerCount: 1, answers: [ANSWERS[0]] });

    await guess(userEvent.setup(), "France");

    expectResult("First guess! +100");
  });

  it("says correct for a single-answer round won after a wrong guess", async () => {
    renderGame({ answerCount: 1, answers: [ANSWERS[0]] });
    const user = userEvent.setup();

    await guess(user, "Italy");
    await guess(user, "France");

    expectResult("Correct! +100");
    expect(screen.queryByText(/First guess/)).not.toBeInTheDocument();
  });

  it("shows the hints taken before a refresh, and what the round is now worth", () => {
    renderGame({ hints, round: makeRoundProps({ hintCount: 3, hintsTakenAt: [0, 0] }) });

    expect(screen.getByText("Clue 0")).toBeInTheDocument();
    expect(screen.getByText("Clue 1")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hint .*· 1 left/ })).toBeInTheDocument();
    expect(screen.getByText("Worth 67")).toBeInTheDocument();
  });
});
