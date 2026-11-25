import { act, renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import { resetChallengeStorageForTests, saveChallengeScore, useChallengeScore } from "./challengeStorage";

beforeEach(() => {
  localStorage.clear();
  resetChallengeStorageForTests();
});

describe("challengeStorage", () => {
  it("finds the score saved for a game, and none for another", () => {
    saveChallengeScore({ gameId: "g1", score: 320, maxScore: 500 });

    expect(renderHook(() => useChallengeScore("g1")).result.current).toEqual({
      gameId: "g1",
      score: 320,
      maxScore: 500,
    });
    expect(renderHook(() => useChallengeScore("g2")).result.current).toBeNull();
  });

  it("updates a mounted hook when a score is saved", () => {
    const { result } = renderHook(() => useChallengeScore("g1"));
    expect(result.current).toBeNull();

    act(() => saveChallengeScore({ gameId: "g1", score: 10, maxScore: 100 }));

    expect(result.current?.score).toBe(10);
  });

  it("keeps only the last 20 games", () => {
    for (let i = 0; i < 25; i++) saveChallengeScore({ gameId: "g" + i, score: i, maxScore: 100 });

    expect(JSON.parse(localStorage.getItem("gg-challenges")!)).toHaveLength(20);
    expect(renderHook(() => useChallengeScore("g0")).result.current).toBeNull();
    expect(renderHook(() => useChallengeScore("g24")).result.current?.score).toBe(24);
  });

  it("ignores stored text it can't read", () => {
    localStorage.setItem("gg-challenges", "not json");
    expect(renderHook(() => useChallengeScore("g1")).result.current).toBeNull();
    localStorage.setItem(
      "gg-challenges",
      JSON.stringify([{ gameId: 1 }, null, { gameId: "g1", score: 5, maxScore: 9 }]),
    );
    expect(renderHook(() => useChallengeScore("g1")).result.current?.score).toBe(5);
  });
});
