import { describe, expect, it } from "vitest";
import { makeRiverRound, makeRound } from "../../test/fixtures";
import { GAME_OPTIONS } from "../gameTypes";
import { answerNames, gameTypeLabel } from "./answers";

describe("answerNames", () => {
  it("lists the three tripoint countries in blob order", () => {
    expect(answerNames(makeRound())).toEqual(["Belgium", "Netherlands", "Germany"]);
  });

  it("lists the river name", () => {
    expect(answerNames(makeRiverRound())).toEqual(["Danube"]);
  });

  it("returns nothing for an unknown game type", () => {
    expect(answerNames(makeRound({ gameType: { name: "SOMETHING_ELSE", answerCount: 1 } }))).toEqual([]);
  });
});

describe("gameTypeLabel", () => {
  it("uses the singular label of every known game option", () => {
    for (const option of GAME_OPTIONS) {
      expect(gameTypeLabel(option.name)).toBe(option.singular);
    }
  });

  it("returns an unknown name as it is", () => {
    expect(gameTypeLabel("SOMETHING_ELSE")).toBe("SOMETHING_ELSE");
  });
});
