import { vi } from "vitest";
import type { GameRoundResponse } from "../api/types";
import type { AnswerState, Round } from "../games/common/Model";
import { roundPoints } from "../games/common/scoring";
import type { CountryShape, CountryShapeRound } from "../games/countries/model";
import type { Lake, LakeRound } from "../games/lakes/model";
import type { River, RiverRound } from "../games/rivers/model";
import type { TripointModel } from "../games/tripoint/model";

export function makeRoundProps(overrides: Partial<Round> = {}): Round {
  return {
    nextRound: vi.fn(),
    goToRound: vi.fn(),
    showSummary: vi.fn(),
    hasNextRound: true,
    currentRound: 0,
    totalRoundCount: 3,
    answerState: "UNANSWERED",
    hintCount: 3,
    guessLimit: 5,
    hintsUsed: 0,
    points: 0,
    score: 0,
    maxScore: 300,
    perfectRounds: 0,
    gameCompleted: false,
    roundAnswerStates: ["UNANSWERED", "UNANSWERED", "UNANSWERED"],
    roundPoints: [0, 0, 0],
    ...overrides,
  };
}

function square(offset: number) {
  return {
    type: "Feature",
    properties: { name: "square" },
    geometry: {
      type: "Polygon",
      coordinates: [[[offset, offset], [offset + 1, offset], [offset + 1, offset + 1], [offset, offset]]],
    },
  };
}

/** Three countries that meet at a point. The Netherlands is also known as Holland. */
export function makeTripointData(): TripointModel {
  return {
    coordinate: { latitude: 50.75, longitude: 6.02 },
    countries: [
      { name: "Belgium", additionalNames: [], geoData: square(0) },
      { name: "Netherlands", additionalNames: ["Holland"], geoData: square(1) },
      { name: "Germany", additionalNames: ["Deutschland"], geoData: square(2) },
    ],
  } as unknown as TripointModel;
}

export function makeRiverData(): River {
  return {
    id: "Q1653",
    name: "Danube",
    additionalNames: ["Donau"],
    continent: "Europe",
    source: { latitude: 48.0, longitude: 8.1 },
    mouth: { latitude: 45.2, longitude: 29.7 },
    countries: ["Romania", "Germany", "Austria"],
    geoData: {
      type: "Feature",
      properties: { name: "Danube" },
      geometry: { type: "MultiLineString", coordinates: [[[0, 0], [1, 1]]] },
    },
  } as unknown as River;
}

/** The data of a round of any river game. */
export function makeRiverRoundData(overrides: Partial<RiverRound> = {}): RiverRound {
  return { river: makeRiverData(), countries: [], ...overrides };
}

/** A square lake in Russia. */
export function makeLakeData(): Lake {
  return {
    id: "Q5513",
    name: "Lake Baikal",
    additionalNames: ["Baikal"],
    continent: "Asia",
    countries: ["Russia"],
    geoData: {
      type: "Feature",
      properties: { name: "Lake Baikal" },
      geometry: { type: "Polygon", coordinates: [[[0, 0], [1, 0], [1, 1], [0, 1], [0, 0]]] },
    },
  } as unknown as Lake;
}

/** The data of a round of the lake game. */
export function makeLakeRoundData(overrides: Partial<LakeRound> = {}): LakeRound {
  return { lake: makeLakeData(), ...overrides };
}

function lines(offset: number) {
  return {
    type: "Feature",
    properties: { name: "lines" },
    geometry: { type: "MultiLineString", coordinates: [[[offset, offset], [offset + 1, offset + 1]]] },
  };
}

/** The Netherlands (also Holland): borders Germany and Belgium, and France through Saint Martin. */
export function makeCountryShapeData(overrides: Partial<CountryShape> = {}): CountryShape {
  return {
    name: "Netherlands",
    additionalNames: ["Holland"],
    coastKm: 451,
    landBorderKm: 1027,
    neighbours: [
      { name: "Germany", km: 577, mainland: true },
      { name: "Belgium", km: 450, mainland: true },
      { name: "France", km: 10, mainland: false },
    ],
    focus: { west: 3.3, south: 50.7, east: 7.2, north: 53.6 },
    outline: square(1),
    coast: lines(2),
    landBorders: lines(3),
    ...overrides,
  } as unknown as CountryShape;
}

/** The data of a round of any country-shape game. */
export function makeCountryShapeRoundData(overrides: Partial<CountryShapeRound> = {}): CountryShapeRound {
  return {
    country: makeCountryShapeData(),
    neighbours: [
      { name: "Germany", outline: square(4) },
      { name: "Belgium", outline: square(5) },
    ],
    ...overrides,
  } as unknown as CountryShapeRound;
}

export function makeRound(overrides: Partial<GameRoundResponse> = {}): GameRoundResponse {
  return {
    jsonBlob: JSON.stringify(makeTripointData()),
    totalRoundCount: 3,
    gameType: { name: "TRIPOINT", answerCount: 3, hintCount: 3, guessLimit: 5 },
    answerState: "UNANSWERED",
    hintsUsed: 0,
    points: 0,
    gameResult: {
      gameState: "CREATED",
      roundsUnanswered: 3,
      score: 0,
      roundAnswerStates: ["UNANSWERED", "UNANSWERED", "UNANSWERED"],
      maxScore: 300,
      roundsCorrect: 0,
      perfectRounds: 0,
      roundPoints: [0, 0, 0],
      roundHintsUsed: [0, 0, 0],
    },
    ...overrides,
  };
}

export function makeRiverRound(overrides: Partial<GameRoundResponse> = {}): GameRoundResponse {
  return makeRound({
    jsonBlob: JSON.stringify(makeRiverRoundData()),
    gameType: { name: "RIVERS_BY_SHAPE", answerCount: 1, hintCount: 3, guessLimit: 5 },
    ...overrides,
  });
}

/** The server's response for a Lake Shapes round. */
export function makeLakeRound(overrides: Partial<GameRoundResponse> = {}): GameRoundResponse {
  return makeRound({
    jsonBlob: JSON.stringify(makeLakeRoundData()),
    gameType: { name: "LAKES_BY_SHAPE", answerCount: 1, hintCount: 3, guessLimit: 5 },
    ...overrides,
  });
}

export interface RoundSpec {
  /** A River Shapes round (3 hints) instead of a Tripoint round (3 hints). */
  river?: boolean;
  answerState: AnswerState;
  hintsUsed?: number;
}

/** The server's responses for a whole game, each carrying the same complete result. */
export function makeGameRounds(specs: RoundSpec[]): GameRoundResponse[] {
  const hintCounts = specs.map(() => 3);
  const hintsUsed = specs.map(spec => spec.hintsUsed ?? 0);
  const points = specs.map((spec, i) => roundPoints(spec.answerState, hintsUsed[i], hintCounts[i]));
  const unanswered = specs.filter(spec => spec.answerState === "UNANSWERED").length;
  const gameResult = {
    gameState: unanswered === 0 ? "FINISHED" : unanswered === specs.length ? "CREATED" : "IN_PROGRESS",
    roundsUnanswered: unanswered,
    score: points.reduce((sum, p) => sum + p, 0),
    roundAnswerStates: specs.map(spec => spec.answerState),
    maxScore: 100 * specs.length,
    roundsCorrect: specs.filter(spec => spec.answerState === "CORRECT").length,
    perfectRounds: specs.filter((spec, i) => spec.answerState === "CORRECT" && hintsUsed[i] === 0).length,
    roundPoints: points,
    roundHintsUsed: hintsUsed,
  } as GameRoundResponse["gameResult"];
  return specs.map((spec, i) => {
    const make = spec.river ? makeRiverRound : makeRound;
    return make({
      totalRoundCount: specs.length,
      answerState: spec.answerState,
      hintsUsed: hintsUsed[i],
      points: points[i],
      gameResult,
    });
  });
}

/** What GET /api/countries returns. */
export const countryNames = [
  { name: "Belgium", additionalNames: [] as string[] },
  { name: "France", additionalNames: [] as string[] },
  { name: "Germany", additionalNames: ["Deutschland"] },
  { name: "Netherlands", additionalNames: ["Holland"] },
];

/** What GET /api/rivers returns; matches makeRiverData() plus one other river. */
export const riverNames = [
  { name: "Danube", additionalNames: ["Donau"] },
  { name: "Thames", additionalNames: [] as string[] },
];

/** What GET /api/lakes returns; matches makeLakeData() plus one other lake. */
export const lakeNames = [
  { name: "Lake Baikal", additionalNames: ["Baikal"] },
  { name: "Lake Victoria", additionalNames: [] as string[] },
];

/** The server's response for a round of a country-shape game. */
export function makeCountryShapeRound(
  type = "COUNTRIES_BY_NEIGHBOURS",
  overrides: Partial<GameRoundResponse> = {},
): GameRoundResponse {
  return makeRound({
    jsonBlob: JSON.stringify(makeCountryShapeRoundData()),
    gameType: { name: type, answerCount: 1, hintCount: 3, guessLimit: 5 },
    ...overrides,
  });
}
