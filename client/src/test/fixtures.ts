import { vi } from "vitest";
import { GameRoundModel, Round } from "../games/common/Model";
import type { River } from "../games/RiverShapes";
import type { TripointModel } from "../games/Tripoint";

export function makeRoundProps(overrides: Partial<Round> = {}): Round {
  return new Round({
    nextRound: vi.fn(),
    goToRound: vi.fn(),
    showSummary: vi.fn(),
    hasNextRound: true,
    currentRound: 0,
    totalRoundCount: 3,
    answerState: "UNANSWERED",
    score: 0,
    gameCompleted: false,
    roundAnswerStates: ["UNANSWERED", "UNANSWERED", "UNANSWERED"],
    ...overrides,
  });
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
      { name: "Belgium", additionalNames: [], geoData: square(0), showAsHint: false },
      { name: "Netherlands", additionalNames: ["Holland"], geoData: square(1), showAsHint: false },
      { name: "Germany", additionalNames: ["Deutschland"], geoData: square(2), showAsHint: false },
    ],
  } as unknown as TripointModel;
}

export function makeRiverData(): River {
  return {
    name: "Danube",
    additionalNames: ["Donau"],
    continent: "Europe",
    geoData: {
      type: "Feature",
      properties: {},
      geometry: { type: "MultiLineString", coordinates: [[[0, 0], [1, 1]]] },
    },
  } as unknown as River;
}

export function makeRound(overrides: Partial<GameRoundModel> = {}): GameRoundModel {
  return {
    jsonBlob: JSON.stringify(makeTripointData()),
    totalRoundCount: 3,
    gameType: { name: "TRIPOINT", answerCount: 3 },
    answerState: "UNANSWERED",
    gameResult: {
      gameState: "CREATED",
      roundsUnanswered: 3,
      score: 0,
      roundAnswerStates: ["UNANSWERED", "UNANSWERED", "UNANSWERED"],
    },
    ...overrides,
  };
}

export function makeRiverRound(overrides: Partial<GameRoundModel> = {}): GameRoundModel {
  return makeRound({
    jsonBlob: JSON.stringify(makeRiverData()),
    gameType: { name: "RIVERS_BY_SHAPE", answerCount: 1 },
    ...overrides,
  });
}

/** What GET /api/countries returns. */
export const countryNames = [
  { name: "Belgium", additionalNames: [] as string[] },
  { name: "France", additionalNames: [] as string[] },
  { name: "Germany", additionalNames: ["Deutschland"] },
  { name: "Netherlands", additionalNames: ["Holland"] },
];
