import { describe, expect, it } from "vitest";
import { makeRiverRoundData, makeTripointData } from "../test/fixtures";
import { findGameModule, GAME_MODULES, MENU_OPTIONS, RANDOM_OPTION, ROUND_OPTIONS } from "./registry";

describe("GAME_MODULES", () => {
  it("uses the names the backend understands", () => {
    expect(GAME_MODULES.map((m) => m.type)).toEqual(["TRIPOINT", "RIVERS_BY_SHAPE", "RIVERS_ON_MAP"]);
  });

  it("has a unique type per module", () => {
    expect(new Set(GAME_MODULES.map((m) => m.type)).size).toBe(GAME_MODULES.length);
  });

  it("gives every module a title, description and icon", () => {
    for (const game of GAME_MODULES) {
      expect(game.title).not.toBe("");
      expect(game.singular).not.toBe("");
      expect(game.description).not.toBe("");
      expect(game.Icon).toBeDefined();
      expect(game.Component).toBeDefined();
    }
  });

  it("lists the answers of a tripoint in country order", () => {
    expect(findGameModule("TRIPOINT")?.answerNames(makeTripointData())).toEqual(["Belgium", "Netherlands", "Germany"]);
  });

  it("lists the name of a river", () => {
    expect(findGameModule("RIVERS_BY_SHAPE")?.answerNames(makeRiverRoundData())).toEqual(["Danube"]);
  });
});

describe("findGameModule", () => {
  it("finds a module by its type", () => {
    expect(findGameModule("RIVERS_BY_SHAPE")?.singular).toBe("River shape");
  });

  it("returns undefined for an unknown type, including the request-only RANDOM", () => {
    expect(findGameModule("SOMETHING_ELSE")).toBeUndefined();
    expect(findGameModule("RANDOM")).toBeUndefined();
  });
});

describe("MENU_OPTIONS", () => {
  it("offers every game followed by a mix of them", () => {
    expect(MENU_OPTIONS.map((o) => o.type)).toEqual(["TRIPOINT", "RIVERS_BY_SHAPE", "RIVERS_ON_MAP", "RANDOM"]);
    expect(MENU_OPTIONS[MENU_OPTIONS.length - 1]).toBe(RANDOM_OPTION);
  });

  it("gives every option a title, description and icon", () => {
    for (const option of MENU_OPTIONS) {
      expect(option.title).not.toBe("");
      expect(option.description).not.toBe("");
      expect(option.Icon).toBeDefined();
    }
  });
});

describe("ROUND_OPTIONS", () => {
  it("stays within the backend limit of 1 to 10 rounds", () => {
    for (const rounds of ROUND_OPTIONS) {
      expect(rounds).toBeGreaterThanOrEqual(1);
      expect(rounds).toBeLessThanOrEqual(10);
    }
  });
});
