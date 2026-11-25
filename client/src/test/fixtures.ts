import { vi } from "vitest";
import type {
  DailyGameResponse,
  DailyChallenge,
  DailyScore,
  GameRoundResponse,
  RoundAnswerResponse,
  RoundGuessesResponse,
  RoundHintsResponse,
} from "../api/types";
import type { AnswerState, Round } from "../games/common/Model";
import { roundPoints } from "../games/common/scoring";
import type { CountryCitiesRound, CountryRiversRound, CountryShape, CountryShapeRound } from "../games/countries/model";
import type { CountryPairRound } from "../games/pairs/model";
import type { Lake, LakeRound } from "../games/lakes/model";
import type { River, RiverRound } from "../games/rivers/model";
import type { TripointModel } from "../games/tripoint/model";
import { ContinentIcon } from "../ui/icons";

export function makeRoundProps(overrides: Partial<Round> = {}): Round {
  return {
    kind: { title: "Test Game", Icon: ContinentIcon },
    answerSave: "saved",
    retryAnswer: vi.fn(),
    nextRound: vi.fn(),
    goToRound: vi.fn(),
    showSummary: vi.fn(),
    hasNextRound: true,
    currentRound: 0,
    totalRoundCount: 3,
    answerState: "UNANSWERED",
    hintCount: 3,
    guessLimit: 5,
    hasClues: false,
    difficulty: "NORMAL",
    continents: [],
    hintsUsed: 0,
    guesses: [],
    hintsTakenAt: [],
    recordGuess: vi.fn(),
    recordHint: vi.fn(),
    points: 0,
    score: 0,
    maxScore: 300,
    gameCompleted: false,
    roundAnswerStates: ["UNANSWERED", "UNANSWERED", "UNANSWERED"],
    roundPoints: [0, 0, 0],
    roundHintsUsed: [0, 0, 0],
    roundFirstGuess: [false, false, false],
    ...overrides,
  };
}

function square(offset: number) {
  return {
    type: "Feature",
    properties: { name: "square" },
    geometry: {
      type: "Polygon",
      coordinates: [
        [
          [offset, offset],
          [offset + 1, offset],
          [offset + 1, offset + 1],
          [offset, offset],
        ],
      ],
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
      geometry: {
        type: "MultiLineString",
        coordinates: [
          [
            [0, 0],
            [1, 1],
          ],
        ],
      },
    },
  } as unknown as River;
}

/** The data of a round of any river game. */
export function makeRiverRoundData(overrides: Partial<RiverRound> = {}): RiverRound {
  return { river: makeRiverData(), countries: [], focus: null, ...overrides };
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
      geometry: {
        type: "Polygon",
        coordinates: [
          [
            [0, 0],
            [1, 0],
            [1, 1],
            [0, 1],
            [0, 0],
          ],
        ],
      },
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
    geometry: {
      type: "MultiLineString",
      coordinates: [
        [
          [offset, offset],
          [offset + 1, offset + 1],
        ],
      ],
    },
  };
}

/** The Netherlands (also Holland): borders Germany and Belgium, and France through Saint Martin. */
export function makeCountryShapeData(overrides: Partial<CountryShape> = {}): CountryShape {
  return {
    name: "Netherlands",
    additionalNames: ["Holland"],
    continents: ["Europe"],
    coastKm: 451,
    landBorderKm: 1027,
    areaKm2: 41_000,
    neighbours: [
      { name: "Germany", km: 577, mainland: true, lines: [0] },
      { name: "Belgium", km: 450, mainland: true, lines: [] },
      { name: "France", km: 10, mainland: false, lines: [] },
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

/**
 * The data of a round of City Map: the Netherlands and three of its cities, largest first.
 * The country's layers start at: outline 1,1; coast 2,2; land borders 3,3.
 */
export function makeCountryCitiesRoundData(overrides: Partial<CountryCitiesRound> = {}): CountryCitiesRound {
  return {
    country: makeCountryShapeData(),
    cities: [
      { name: "Amsterdam", lon: 4.9, lat: 52.37, population: 1_000_000, capital: true },
      { name: "Rotterdam", lon: 4.48, lat: 51.92, population: 250_000, capital: false },
      { name: "Utrecht", lon: 5.12, lat: 52.09, population: 0, capital: false },
    ],
    ...overrides,
  } as CountryCitiesRound;
}

/**
 * The data of a round of Waterways: the Netherlands, with its major rivers, minor rivers and a
 * lake. The layers start at: outline 1,1; coast 2,2; land borders 3,3; major rivers 10,10; minor
 * rivers 11,11; lakes 12,12.
 */
export function makeCountryRiversRoundData(overrides: Partial<CountryRiversRound> = {}): CountryRiversRound {
  return {
    country: makeCountryShapeData(),
    major: lines(10),
    minor: lines(11),
    lakes: square(12),
    longestRiver: "Rhine",
    ...overrides,
  } as unknown as CountryRiversRound;
}

/**
 * The data of a round of a pair game: Belgium and the Netherlands, who border each other. The layers
 * start at: Belgium outline 1,1, coast 2,2, land borders 3,3; the Netherlands outline 4,4, coast 5,5,
 * land borders 6,6; the shared border 7,7; the neighbours 8,8 (Germany) and 9,9 (France).
 */
export function makeCountryPairRoundData(overrides: Partial<CountryPairRound> = {}): CountryPairRound {
  return {
    countries: [
      makeCountryShapeData({
        name: "Belgium",
        additionalNames: [],
        outline: square(1),
        coast: lines(2),
        landBorders: lines(3),
      } as Partial<CountryShape>),
      makeCountryShapeData({
        name: "Netherlands",
        additionalNames: ["Holland"],
        outline: square(4),
        coast: lines(5),
        landBorders: lines(6),
      } as Partial<CountryShape>),
    ],
    sharedBorder: lines(7),
    borderKm: 450,
    neighbours: [
      { name: "Germany", outline: square(8) },
      { name: "France", outline: square(9) },
    ],
    focus: { west: 2.5, south: 49.5, east: 7.2, north: 53.6 },
    borderBounds: { west: 3.9, south: 50.7, east: 6.4, north: 51.5 },
    ...overrides,
  } as unknown as CountryPairRound;
}

export function makeRound(overrides: Partial<GameRoundResponse> = {}): GameRoundResponse {
  return {
    jsonBlob: JSON.stringify(makeTripointData()),
    totalRoundCount: 3,
    gameType: { name: "TRIPOINT", answerCount: 3, hintCount: 3, guessLimit: 5 },
    answerState: "UNANSWERED",
    hintsUsed: 0,
    points: 0,
    guesses: [],
    hintsTakenAt: [],
    difficulty: "NORMAL",
    continents: [],
    gameResult: {
      gameState: "CREATED",
      roundsUnanswered: 3,
      score: 0,
      roundAnswerStates: ["UNANSWERED", "UNANSWERED", "UNANSWERED"],
      maxScore: 300,
      roundsCorrect: 0,
      roundPoints: [0, 0, 0],
      roundHintsUsed: [0, 0, 0],
      roundFirstGuess: [false, false, false],
      firstGuessRounds: 0,
    },
    ...overrides,
  };
}

/** What POST /api/games/{id}/rounds/{round}/answers returns: no round data. */
export function makeAnswerResponse(overrides: Partial<RoundAnswerResponse> = {}): RoundAnswerResponse {
  const { answerState, hintsUsed, points, gameResult } = makeRound({
    answerState: "CORRECT",
    points: 100,
    gameResult: {
      gameState: "IN_PROGRESS",
      roundsUnanswered: 2,
      score: 100,
      roundAnswerStates: ["CORRECT", "UNANSWERED", "UNANSWERED"],
      maxScore: 300,
      roundsCorrect: 1,
      roundPoints: [100, 0, 0],
      roundHintsUsed: [0, 0, 0],
      roundFirstGuess: [true, false, false],
      firstGuessRounds: 1,
    },
  });
  return { answerState, hintsUsed, points, gameResult, ...overrides };
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
  /** For a revealed round: how many of its answers were found (a tripoint has 3, a river 1). */
  answersFound?: number;
  /** Correct with no hints and no wrong guess. */
  firstGuess?: boolean;
  /** The wrong guesses recorded for the round. */
  wrongGuesses?: string[];
}

/** The server's responses for a whole game, each carrying the same complete result. */
export function makeGameRounds(specs: RoundSpec[]): GameRoundResponse[] {
  const hintsUsed = specs.map((spec) => spec.hintsUsed ?? 0);
  const points = specs.map((spec, i) =>
    roundPoints(
      spec.answerState,
      hintsUsed[i],
      spec.answersFound ?? 0,
      spec.river ? 1 : 3,
      spec.wrongGuesses?.length ?? 0,
    ),
  );
  const unanswered = specs.filter((spec) => spec.answerState === "UNANSWERED").length;
  const gameResult = {
    gameState: unanswered === 0 ? "FINISHED" : unanswered === specs.length ? "CREATED" : "IN_PROGRESS",
    roundsUnanswered: unanswered,
    score: points.reduce((sum, p) => sum + p, 0),
    roundAnswerStates: specs.map((spec) => spec.answerState),
    maxScore: 10 * specs.length,
    roundsCorrect: specs.filter((spec) => spec.answerState === "CORRECT").length,
    roundPoints: points,
    roundHintsUsed: hintsUsed,
    roundFirstGuess: specs.map((spec) => spec.firstGuess ?? false),
    firstGuessRounds: specs.filter((spec) => spec.firstGuess).length,
  } as GameRoundResponse["gameResult"];
  return specs.map((spec, i) => {
    const make = spec.river ? makeRiverRound : makeRound;
    return make({
      totalRoundCount: specs.length,
      answerState: spec.answerState,
      hintsUsed: hintsUsed[i],
      points: points[i],
      guesses: (spec.wrongGuesses ?? []).map((text) => ({ text, correct: false })),
      gameResult,
    });
  });
}

/** What GET /api/countries returns. */
export const countryNames = [
  { name: "Belgium", additionalNames: [] as string[], lat: 50.64, lon: 4.67 },
  { name: "France", additionalNames: [] as string[], lat: 46.6, lon: 2.45 },
  { name: "Germany", additionalNames: ["Deutschland"], lat: 51.1, lon: 10.4 },
  { name: "Netherlands", additionalNames: ["Holland"], lat: 52.2, lon: 5.6 },
];

/** What GET /api/rivers returns; matches makeRiverData() plus one other river. */
export const riverNames = [
  { name: "Danube", additionalNames: ["Donau"], lat: 45.2, lon: 19.9 },
  { name: "Thames", additionalNames: [] as string[], lat: 51.6, lon: -1.2 },
];

/** What GET /api/lakes returns; matches makeLakeData() plus one other lake. */
export const lakeNames = [
  { name: "Lake Baikal", additionalNames: ["Baikal"], lat: 53.5, lon: 108.2 },
  { name: "Lake Victoria", additionalNames: [] as string[], lat: -1.0, lon: 33.0 },
];

/** The server's response for a City Map round. */
export function makeCountryCitiesRound(overrides: Partial<GameRoundResponse> = {}): GameRoundResponse {
  return makeRound({
    jsonBlob: JSON.stringify(makeCountryCitiesRoundData()),
    gameType: { name: "COUNTRIES_BY_CITIES", answerCount: 1, hintCount: 3, guessLimit: 5 },
    ...overrides,
  });
}

/** The server's response for a Waterways round. */
export function makeCountryRiversRound(overrides: Partial<GameRoundResponse> = {}): GameRoundResponse {
  return makeRound({
    jsonBlob: JSON.stringify(makeCountryRiversRoundData()),
    gameType: { name: "COUNTRIES_BY_RIVERS", answerCount: 1, hintCount: 3, guessLimit: 5 },
    ...overrides,
  });
}

/** The server's response for a round of a pair game. */
export function makeCountryPairRound(
  type = "BORDERS_BY_SHAPE",
  overrides: Partial<GameRoundResponse> = {},
): GameRoundResponse {
  return makeRound({
    jsonBlob: JSON.stringify(makeCountryPairRoundData()),
    gameType: { name: type, answerCount: 2, hintCount: 3, guessLimit: 5 },
    ...overrides,
  });
}

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

/** What POST /api/games/{id}/rounds/{round}/guesses returns: the round's guesses so far. */
export function makeGuessesResponse(overrides: Partial<RoundGuessesResponse> = {}): RoundGuessesResponse {
  return { guesses: [{ text: "France", correct: false }], ...overrides };
}

/** What POST /api/games/{id}/rounds/{round}/hints returns: for each hint, how many guesses came before it. */
export function makeHintsResponse(overrides: Partial<RoundHintsResponse> = {}): RoundHintsResponse {
  return { hintsTakenAt: [0, 1], ...overrides };
}

/** What GET /api/daily-scores/{date} returns. */
export function makeDailyScore(overrides: Partial<DailyScore> = {}): DailyScore {
  return {
    date: "2026-11-18",
    difficulty: "NORMAL",
    players: 4,
    averageScore: 312.5,
    maxScore: 500,
    buckets: [0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 1],
    ...overrides,
  };
}

/** What GET /api/daily-challenges/{date} returns. */
export function makeDailyChallenge(overrides: Partial<DailyChallenge> = {}): DailyChallenge {
  return {
    date: "2026-11-18",
    number: 2,
    gameTypes: ["TRIPOINT", "LAKES_BY_SHAPE", "RIVERS_BY_SHAPE", "BORDERS_BY_SHAPE", "COUNTRIES_BY_CITIES"],
    ...overrides,
  };
}

/** What POST /api/daily-games returns. */
export function makeDailyGameResponse(overrides: Partial<DailyGameResponse> = {}): DailyGameResponse {
  return { id: "0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d", date: "2026-11-18", number: 2, ...overrides };
}
