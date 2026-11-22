import { readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { normaliseString } from "../helper/stringHelper";
import { coastHint, firstLetterHint as countryFirstLetter, longestRiverHint } from "./countries/countryHints";
import type { CountryShape } from "./countries/model";
import { lakeHints } from "./lakes/lakeHints";
import type { Lake } from "./lakes/model";
import type { River } from "./rivers/model";
import { continentHint, firstLetterHint as riverFirstLetter, mainCountryHint } from "./rivers/riverHints";

const PROCESSED = join(process.cwd(), "..", "src", "main", "resources", "processed");

// Words that say what kind of place it is; naming them tells nothing.
const IGNORED = new Set(["lake", "lago", "lac", "loch", "lough", "river", "rio", "sea", "the", "of", "de", "del"]);

function load<T>(folder: string): T[] {
  return readdirSync(join(PROCESSED, folder))
    .filter((file) => file.endsWith(".json") && file !== "index.json")
    .map((file) => JSON.parse(readFileSync(join(PROCESSED, folder, file), "utf8")) as T);
}

function words(text: string): string[] {
  return text
    .split(/[\s-]+/)
    .filter((word) => !IGNORED.has(word.toLowerCase()))
    .map(normaliseString)
    .filter((word) => word.length >= 3);
}

/** The hints that contain a word of the answer's name or aliases. */
function leaks(names: string[], hints: (string | null)[]): string[] {
  const answerWords = new Set(names.flatMap(words));
  return hints.filter((hint): hint is string => !!hint && words(hint).some((word) => answerWords.has(word)));
}

describe("hints never show a word of the answer", () => {
  it("for rivers", () => {
    const found = load<River>("rivers").flatMap((river) =>
      leaks(
        [river.name, ...river.additionalNames],
        [continentHint(river), mainCountryHint(river), riverFirstLetter(river)],
      ).map((hint) => `${river.name}: ${hint}`),
    );
    expect(found).toEqual([]);
  });

  it("for lakes", () => {
    const found = load<Lake>("lakes").flatMap((lake) =>
      [lakeHints(lake, false), lakeHints(lake, true)].flatMap((config) =>
        leaks(
          [lake.name, ...lake.additionalNames],
          [0, 1, 2].map((i) => config.text?.(i, []) ?? null),
        ).map((hint) => `${lake.name}: ${hint}`),
      ),
    );
    expect(found).toEqual([]);
  });

  it("for country rivers", () => {
    const found = load<{ name: string; longestRiver: string | null }>("country-rivers").flatMap((entry) => {
      const country = { name: entry.name, additionalNames: [], coastKm: 1 } as unknown as CountryShape;
      const round = { country, longestRiver: entry.longestRiver };
      return leaks([entry.name], [longestRiverHint(round), coastHint(round), countryFirstLetter(round)]).map(
        (hint) => `${entry.name}: ${hint}`,
      );
    });
    expect(found).toEqual([]);
  });
});
