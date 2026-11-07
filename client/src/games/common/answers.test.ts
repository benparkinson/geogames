import { describe, expect, it } from "vitest";
import { matchAnswer } from "./answers";

describe("matchAnswer", () => {
  const candidates = [
    { name: "Germany", additionalNames: ["Deutschland", "Federal Republic of Germany"] },
    { name: "Côte d'Ivoire", additionalNames: ["Ivory Coast"] },
  ];

  it("returns the canonical name for a name, ignoring case, accents and punctuation", () => {
    expect(matchAnswer("germany", candidates)).toBe("Germany");
    expect(matchAnswer("cote d ivoire", candidates)).toBe("Côte d'Ivoire");
  });

  it("returns the canonical name for an alias", () => {
    expect(matchAnswer("DEUTSCHLAND", candidates)).toBe("Germany");
    expect(matchAnswer("ivory-coast", candidates)).toBe("Côte d'Ivoire");
  });

  it("returns null when nothing matches", () => {
    expect(matchAnswer("France", candidates)).toBeNull();
    expect(matchAnswer("Germ", candidates)).toBeNull();
  });

  it("returns null when there are no candidates", () => {
    expect(matchAnswer("Germany", [])).toBeNull();
  });
});
