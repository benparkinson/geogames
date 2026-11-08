import { describe, expect, it } from "vitest";
import contract from "../../../contract/api-shapes.json";
import {
  countryNames,
  lakeNames,
  makeCountryShapeRound,
  makeLakeRound,
  makeRiverRound,
  makeRiverRoundData,
  makeRound,
  makeTripointData,
  riverNames,
} from "../test/fixtures";
import { shapeOf, type Shape } from "../test/shape";
import type { GameRoundResponse } from "./types";

/**
 * The frontend's fixtures against the JSON shapes the backend really sends (`contract/api-shapes.json`,
 * written by the backend's ApiContractIT). If the backend changes a response, ApiContractIT fails first;
 * regenerating the file then fails this test until the fixtures (and the code that reads them) are updated.
 *
 * Two checks per response:
 * 1. every key the fixture has exists on the backend with the same type, so the fixtures are not inventing fields;
 * 2. every key the backend sends is either in the fixture or listed below with a reason, so nothing new
 *    appears unnoticed.
 */

/**
 * Backend fields the frontend does not read, and so the fixtures leave out. Paths start at the round
 * response (or the endpoint's list element); `[]` is an array's element.
 */
const IGNORED_BACKEND_FIELDS: Record<string, string> = {
  "jsonBlob.gameType": "Repeats the response's own gameType; the games read the outer one",
};

const rounds = contract.rounds as unknown as Record<string, Shape>;
const endpoints = contract.endpoints as unknown as Record<string, Shape>;

function roundFixtures(): Record<string, GameRoundResponse> {
  const riverOf = (name: string) => makeRiverRound({ gameType: { name, answerCount: 1, hintCount: 2, guessLimit: 5 } });
  const countriesRiver = makeRiverRound({
    gameType: { name: "RIVERS_BY_COUNTRIES", answerCount: 1, hintCount: 2, guessLimit: 5 },
    jsonBlob: JSON.stringify(makeRiverRoundData({ countries: makeTripointData().countries })),
  });
  return {
    TRIPOINT: makeRound(),
    RIVERS_BY_SHAPE: riverOf("RIVERS_BY_SHAPE"),
    RIVERS_ON_MAP: riverOf("RIVERS_ON_MAP"),
    RIVERS_BY_ENDS: riverOf("RIVERS_BY_ENDS"),
    RIVERS_BY_COUNTRIES: countriesRiver,
    COUNTRIES_BY_NEIGHBOURS: makeCountryShapeRound("COUNTRIES_BY_NEIGHBOURS"),
    COUNTRIES_BY_COASTLINE: makeCountryShapeRound("COUNTRIES_BY_COASTLINE"),
    COUNTRIES_BY_LAND_BORDERS: makeCountryShapeRound("COUNTRIES_BY_LAND_BORDERS"),
    LAKES_BY_SHAPE: makeLakeRound(),
  };
}

/** The shape of a round response the way the backend contract describes it: the blob parsed and shaped. */
function roundShape(round: GameRoundResponse): Shape {
  const { jsonBlob, ...rest } = round;
  return { ...(shapeOf(rest) as { [key: string]: Shape }), jsonBlob: shapeOf(JSON.parse(jsonBlob)) };
}

const isUnknown = (shape: Shape) => shape === "empty" || shape === "null";

/** Where the fixture has something the backend does not, or has it as another type. */
function inventedByFixture(fixture: Shape, backend: Shape, path: string, out: string[]): void {
  if (isUnknown(fixture) || backend === "empty") {
    return;
  }
  if (typeof fixture === "string") {
    if (typeof backend !== "string" || !backend.split("|").includes(fixture)) {
      out.push(`${path}: fixture has ${JSON.stringify(fixture)}, backend sends ${JSON.stringify(backend)}`);
    }
    return;
  }
  if (typeof backend === "string") {
    out.push(`${path}: fixture has an object, backend sends ${JSON.stringify(backend)}`);
    return;
  }
  for (const [key, child] of Object.entries(fixture)) {
    const childPath = join(path, key);
    if (key in backend) {
      inventedByFixture(child, backend[key], childPath, out);
    } else {
      out.push(`${childPath}: in the fixture but not sent by the backend`);
    }
  }
}

/** Paths the backend sends that the fixture does not have. An empty fixture array hides its elements. */
function missingFromFixture(fixture: Shape, backend: Shape, path: string, out: string[]): void {
  if (typeof backend === "string") {
    return;
  }
  if (isUnknown(fixture) || typeof fixture === "string") {
    out.push(path);
    return;
  }
  for (const [key, child] of Object.entries(backend)) {
    const childPath = join(path, key);
    if (key in fixture) {
      missingFromFixture(fixture[key], child, childPath, out);
    } else {
      out.push(childPath);
    }
  }
}

function join(path: string, key: string): string {
  if (key === "array") {
    return `${path}[]`;
  }
  return path === "" ? key : `${path}.${key}`;
}

function unexpectedlyMissing(fixture: Shape, backend: Shape): string[] {
  const missing: string[] = [];
  missingFromFixture(fixture, backend, "", missing);
  return missing.filter(path => !(path in IGNORED_BACKEND_FIELDS));
}

describe("the backend contract (contract/api-shapes.json)", () => {
  const fixtures = roundFixtures();

  it("has a fixture for every game type the backend has, and no others", () => {
    expect(Object.keys(fixtures).sort()).toEqual(Object.keys(rounds).sort());
  });

  describe.each(Object.keys(rounds))("%s", type => {
    it("has only fields the backend sends", () => {
      const problems: string[] = [];
      inventedByFixture(roundShape(fixtures[type]), rounds[type], "", problems);
      expect(problems).toEqual([]);
    });

    it("covers every field the backend sends, or ignores it with a reason", () => {
      expect(unexpectedlyMissing(roundShape(fixtures[type]), rounds[type])).toEqual([]);
    });
  });

  it("does not ignore fields the backend no longer sends or the fixtures now have", () => {
    const stillMissing = new Set<string>();
    for (const type of Object.keys(rounds)) {
      const missing: string[] = [];
      missingFromFixture(roundShape(fixtures[type]), rounds[type], "", missing);
      missing.forEach(path => stillMissing.add(path));
    }
    const stale = Object.keys(IGNORED_BACKEND_FIELDS).filter(path => !stillMissing.has(path));
    expect(stale).toEqual([]);
  });

  describe.each([
    ["GET /api/countries", countryNames],
    ["GET /api/rivers", riverNames],
    ["GET /api/lakes", lakeNames],
    ["POST /api/games", { id: 1 }],
  ])("%s", (endpoint, fixture) => {
    it("matches the backend response exactly", () => {
      const backend = endpoints[endpoint];
      const problems: string[] = [];
      inventedByFixture(shapeOf(fixture), backend, "", problems);
      expect(problems).toEqual([]);
      expect(unexpectedlyMissing(shapeOf(fixture), backend)).toEqual([]);
    });
  });
});
