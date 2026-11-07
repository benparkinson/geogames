import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { normaliseString } from "../../helper/stringHelper";
import { makeRoundProps } from "../../test/fixtures";
import MapGame, { HintConfig } from "./MapGame";

const ANSWERS = ["France", "Spain"];

function MapStub({ gameOver, gaveUp }: { data: unknown; gameOver: boolean; gaveUp: boolean }) {
  return <div data-testid="map" data-game-over={String(gameOver)} data-gave-up={String(gaveUp)} />;
}

function renderGame(overrides: Partial<React.ComponentProps<typeof MapGame<object>>> = {}) {
  const submitAnswer = vi.fn();
  const props: React.ComponentProps<typeof MapGame<object>> = {
    data: {},
    answerCount: 2,
    MapComponent: MapStub,
    correctAnswersFunction: () => ANSWERS,
    round: makeRoundProps(),
    prompt: "Name them",
    inputPlaceholder: "Country…",
    submitAnswer,
    answerState: "UNANSWERED",
    isAnswerCorrect: (guess) => ANSWERS.map(normaliseString).includes(normaliseString(guess)),
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
    renderGame({ blankMapCaption: "Shape only." });

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
});

describe("MapGame guessing", () => {
  it("lists a wrong guess without reporting an answer", async () => {
    const { submitAnswer } = renderGame();

    await guess(userEvent.setup(), "Italy");

    expect(screen.getByText('"Italy" isn\'t right. Try again.')).toBeInTheDocument();
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
    const { submitAnswer } = renderGame({ blankMapCaption: "Shape only." });

    await guess(user, "France");
    await guess(user, "Spain");

    expect(submitAnswer).toHaveBeenCalledTimes(1);
    expect(submitAnswer).toHaveBeenCalledWith("CORRECT");
    expect(map()).toHaveAttribute("data-game-over", "true");
    expect(map()).toHaveAttribute("data-gave-up", "false");
    expect(screen.getByRole("button", { name: /Next round/ })).toBeInTheDocument();
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
    expect(screen.queryByText("Shape only.")).not.toBeInTheDocument();
  });

  it("ignores guesses after the round is over", async () => {
    renderGame({ answerState: "CORRECT" });

    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
  });
});

describe("MapGame giving up", () => {
  it("reports GAVE_UP once and reveals every answer", async () => {
    const { submitAnswer } = renderGame();

    await reveal(userEvent.setup());

    expect(submitAnswer).toHaveBeenCalledTimes(1);
    expect(submitAnswer).toHaveBeenCalledWith("GAVE_UP");
    expect(screen.getByText("Answer revealed")).toBeInTheDocument();
    expect(map()).toHaveAttribute("data-game-over", "true");
    expect(map()).toHaveAttribute("data-gave-up", "true");
    expect(screen.getByRole("group", { name: "Answers: 2 of 2 found" })).toBeInTheDocument();
    expect(screen.getByText("France")).toBeInTheDocument();
    expect(screen.getByText("Spain")).toBeInTheDocument();
  });
});

describe("MapGame resuming a finished round", () => {
  it("starts solved with every answer filled in when the round was answered correctly", () => {
    const { submitAnswer } = renderGame({ answerState: "CORRECT" });

    expect(map()).toHaveAttribute("data-game-over", "true");
    expect(map()).toHaveAttribute("data-gave-up", "false");
    expect(screen.getByRole("group", { name: "Answers: 2 of 2 found" })).toBeInTheDocument();
    expect(screen.getByText("Correct!")).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
  });

  it("starts revealed when the round was given up", () => {
    const { submitAnswer } = renderGame({ answerState: "GAVE_UP" });

    expect(map()).toHaveAttribute("data-game-over", "true");
    expect(map()).toHaveAttribute("data-gave-up", "true");
    expect(screen.getByText("Answer revealed")).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();
  });
});

describe("MapGame hints", () => {
  function hintConfig(): HintConfig {
    return {
      total: 2,
      description: "A clue",
      use: vi.fn().mockReturnValueOnce("Continent: Europe").mockReturnValueOnce(null),
    };
  }

  it("calls the hint with an increasing index and shows the text it returns", async () => {
    const user = userEvent.setup();
    const hints = hintConfig();
    renderGame({ hints });

    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    expect(hints.use).toHaveBeenLastCalledWith(0);
    expect(screen.getByText("Continent: Europe")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));
    expect(hints.use).toHaveBeenLastCalledWith(1);
  });

  it("uses up a hint even when it has no text, and stops when none are left", async () => {
    const user = userEvent.setup();
    const hints = hintConfig();
    renderGame({ hints });

    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));

    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
    expect(hints.use).toHaveBeenCalledTimes(2);
    expect(screen.getAllByText(/Continent/)).toHaveLength(1);
  });
});
