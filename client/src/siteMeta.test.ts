import { describe, expect, it } from "vitest";
import { socialMeta, type SocialPage } from "./siteMeta";
import { SITE_URL } from "./siteName";

const REQUIRED = [
  "description",
  "og:type",
  "og:site_name",
  "og:title",
  "og:description",
  "og:url",
  "og:image",
  "og:image:width",
  "og:image:height",
  "og:image:alt",
  "twitter:card",
];

function content(page: SocialPage, key: string): string {
  return socialMeta(page).find((tag) => tag.key === key)?.content ?? "";
}

describe.each<SocialPage>(["site", "daily", "challenge"])("socialMeta(%s)", (page) => {
  it("has each required tag exactly once, keyed by its name or property", () => {
    const tags = socialMeta(page);
    expect(tags.map((tag) => tag.key).sort()).toEqual([...REQUIRED].sort());
    for (const tag of tags) {
      expect(tag.name ?? tag.property).toBe(tag.key);
      expect(tag.content).not.toBe("");
    }
  });

  it("uses absolute URLs on the public site", () => {
    expect(content(page, "og:image")).toBe(`${SITE_URL}/og-image.png`);
    expect(content(page, "og:url").startsWith(`${SITE_URL}/`)).toBe(true);
  });

  it("keeps the description short enough for a card", () => {
    expect(content(page, "description").length).toBeLessThanOrEqual(200);
    expect(content(page, "og:description")).toBe(content(page, "description"));
  });
});

describe("socialMeta", () => {
  it("points the daily and challenge cards at their own pages", () => {
    expect(content("daily", "og:url")).toBe(`${SITE_URL}/daily`);
    expect(content("challenge", "og:url")).toBe(`${SITE_URL}/challenge`);
  });

  it("pitches both the daily challenge and free play for the site", () => {
    expect(content("site", "description")).toContain("daily challenge");
    expect(content("site", "description")).toContain("play freely");
  });
});
