import { describe, expect, it } from "vitest";
import { shareText } from "./shareText";

const result = { score: 430, maxScore: 500, rounds: ["perfect", "hints", "perfect", "revealed", "perfect"] as const };

describe("shareText", () => {
  it("is the title, the score, one square per round and the link", () => {
    expect(
      shareText(
        { date: "2026-11-18", hardcore: false, result: { ...result, rounds: [...result.rounds] } },
        "https://example.org",
      ),
    ).toBe("GeoGauntlet Daily 2026-11-18\n430 / 500\n🟩🟨🟩🟥🟩\nhttps://example.org/daily");
  });

  it("marks Hard mode only when it was on throughout", () => {
    const on = shareText(
      { date: "2026-11-18", hardcore: true, result: { ...result, rounds: [...result.rounds] } },
      "http://localhost:8080",
    );
    const off = shareText(
      { date: "2026-11-18", hardcore: false, result: { ...result, rounds: [...result.rounds] } },
      "http://localhost:8080",
    );

    expect(on.split("\n")[0]).toBe("GeoGauntlet Daily 2026-11-18 · Hard mode");
    expect(off.split("\n")[0]).toBe("GeoGauntlet Daily 2026-11-18");
    expect(on.split("\n").slice(1)).toEqual(off.split("\n").slice(1));
  });

  it("uses the origin it is given and links to /daily, not to a game", () => {
    const text = shareText(
      {
        date: "2026-11-18",
        hardcore: false,
        result: { score: 0, maxScore: 500, rounds: ["revealed", "revealed", "revealed", "revealed", "revealed"] },
      },
      "https://a.b",
    );

    expect(text.endsWith("\nhttps://a.b/daily")).toBe(true);
    expect(text).toContain("🟥🟥🟥🟥🟥");
    expect(text).not.toContain("/games/");
  });
});
