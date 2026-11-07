import { describe, expect, it } from "vitest";
import { makeRoundProps } from "../../test/fixtures";
import { CORRECT_ANSWER, GAVE_UP, Round, UNANSWERED } from "./Model";

describe("Round", () => {
  it("copies every field of its initialiser", () => {
    const init = makeRoundProps({
      currentRound: 2,
      totalRoundCount: 5,
      answerState: CORRECT_ANSWER,
      score: 4,
      hasNextRound: false,
      gameCompleted: true,
      roundAnswerStates: [CORRECT_ANSWER, GAVE_UP, UNANSWERED],
    });

    const round = new Round(init);

    expect(round).toEqual(init);
    expect(round.nextRound).toBe(init.nextRound);
    expect(round.goToRound).toBe(init.goToRound);
    expect(round.showSummary).toBe(init.showSummary);
  });
});

describe("answer state constants", () => {
  it("match the backend enum names", () => {
    expect([CORRECT_ANSWER, GAVE_UP, UNANSWERED]).toEqual(["CORRECT", "GAVE_UP", "UNANSWERED"]);
  });
});
