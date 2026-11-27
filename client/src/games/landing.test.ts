import { describe, expect, it } from "vitest";
import { LANDING, LANDING_TYPES, landingPath } from "./landing";
import { GAME_NAMES } from "./names";
import { GAME_MODULES } from "./registry";

const all = LANDING_TYPES.map((type) => LANDING[type]);
const kebab = (name: string) => name.toLowerCase().replace(/ /g, "-");

describe("game landing pages", () => {
  it("has one for every game, in home page order", () => {
    const standard = GAME_MODULES.filter((module) => !module.extraDifficult);
    const hard = GAME_MODULES.filter((module) => module.extraDifficult);
    expect(LANDING_TYPES).toEqual([...standard, ...hard].map((module) => module.type));
  });

  it("names each slug after its game, in kebab-case", () => {
    for (const type of LANDING_TYPES) {
      expect(LANDING[type].slug).toBe(kebab(GAME_NAMES[type]));
      expect(LANDING[type].slug).toMatch(/^[a-z]+(-[a-z]+)*$/);
      expect(landingPath(type)).toBe(`/play/${LANDING[type].slug}`);
    }
    expect(new Set(all.map((landing) => landing.slug)).size).toBe(all.length);
  });

  it("keeps titles and descriptions short enough for a search result", () => {
    for (const { title, description } of all) {
      expect(title.length).toBeLessThanOrEqual(60);
      expect(description.length).toBeLessThanOrEqual(155);
    }
  });

  it("gives each game its own title, description, heading and introduction", () => {
    for (const texts of [
      all.map((landing) => landing.title),
      all.map((landing) => landing.description),
      all.map((landing) => landing.heading),
      all.flatMap((landing) => landing.intro),
    ]) {
      expect(new Set(texts).size).toBe(texts.length);
    }
    for (const { intro } of all) {
      expect(intro.length).toBeGreaterThanOrEqual(2);
    }
  });

  it("states no hint count or guess limit, which change", () => {
    for (const { howToPlay } of all) {
      expect(howToPlay.join(" ")).not.toMatch(/\d|\b(two|three|four|five|six) (hints|guesses|lives)\b/i);
    }
  });
});
