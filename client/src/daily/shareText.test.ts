import { describe, expect, it } from "vitest";
import { shareText } from "./shareText";

const result = { score: 430, maxScore: 500, rounds: ["perfect", "hints", "perfect", "revealed", "perfect"] as const };

describe("shareText", () => {
  it("is the title, the score, one square per round and the link", () => {
    expect(shareText({ number: 42, hardcore: false, result: { ...result, rounds: [...result.rounds] } }, "https://example.org")).toBe(
      "GeoGauntlet Daily #42\n430 / 500\n🟩🟨🟩🟥🟩\nhttps://example.org/daily",
    );
  });

  it("marks Hardcore only when it was on throughout", () => {
    const on = shareText({ number: 3, hardcore: true, result: { ...result, rounds: [...result.rounds] } }, "http://localhost:8080");
    const off = shareText({ number: 3, hardcore: false, result: { ...result, rounds: [...result.rounds] } }, "http://localhost:8080");

    expect(on.split("\n")[0]).toBe("GeoGauntlet Daily #3 · Hardcore");
    expect(off.split("\n")[0]).toBe("GeoGauntlet Daily #3");
    expect(on.split("\n").slice(1)).toEqual(off.split("\n").slice(1));
  });

  it("uses the origin it is given and links to /daily, not to a game", () => {
    const text = shareText({ number: 1, hardcore: false, result: { score: 0, maxScore: 500, rounds: ["revealed", "revealed", "revealed", "revealed", "revealed"] } }, "https://a.b");

    expect(text.endsWith("\nhttps://a.b/daily")).toBe(true);
    expect(text).toContain("🟥🟥🟥🟥🟥");
    expect(text).not.toContain("/games/");
  });
});
