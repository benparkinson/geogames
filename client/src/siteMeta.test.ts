import { describe, expect, it } from "vitest";
import { INDEXABLE_PATHS, pageMeta, type PageKey } from "./siteMeta";
import { SITE_URL } from "./siteName";
import { LANDING_TYPES, landingPath } from "./games/landing";

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

const INDEXABLE: [PageKey, string][] = [
  ["site", "/"],
  ["daily", "/daily"],
  ["privacy", "/privacy"],
  ...LANDING_TYPES.map((type): [PageKey, string] => [type, landingPath(type)]),
];
const HIDDEN: PageKey[] = ["challenge", "game"];

function content(page: PageKey, key: string): string {
  return pageMeta(page).tags.find((tag) => tag.key === key)?.content ?? "";
}

describe.each<PageKey>([...INDEXABLE.map(([page]) => page), ...HIDDEN])("pageMeta(%s)", (page) => {
  it("has each tag exactly once, keyed by its name or property", () => {
    const { tags } = pageMeta(page);
    const expected = HIDDEN.includes(page) ? [...REQUIRED, "robots"] : REQUIRED;
    expect(tags.map((tag) => tag.key).sort()).toEqual([...expected].sort());
    for (const tag of tags) {
      expect(tag.name ?? tag.property).toBe(tag.key);
      expect(tag.content).not.toBe("");
    }
  });

  it("uses absolute URLs on the public site", () => {
    expect(content(page, "og:image")).toBe(`${SITE_URL}/og-image.png`);
    expect(content(page, "og:url").startsWith(`${SITE_URL}/`)).toBe(true);
  });

  it("keeps the title and description short enough for a search result", () => {
    expect(pageMeta(page).title?.length ?? 0).toBeLessThanOrEqual(60);
    expect(content(page, "description").length).toBeLessThanOrEqual(160);
    expect(content(page, "og:description")).toBe(content(page, "description"));
  });
});

describe("pageMeta", () => {
  it.each(INDEXABLE)("gives %s the canonical URL of %s", (page, path) => {
    expect(pageMeta(page).canonical).toBe(`${SITE_URL}${path}`);
    expect(content(page, "og:url")).toBe(`${SITE_URL}${path}`);
    expect(content(page, "og:title")).toBe(pageMeta(page).title);
  });

  it("lists exactly the indexable paths", () => {
    expect(INDEXABLE_PATHS).toEqual(INDEXABLE.map(([, path]) => path));
  });

  it.each(HIDDEN)("keeps %s out of search results", (page) => {
    expect(content(page, "robots")).toBe("noindex");
    expect(pageMeta(page).canonical).toBeUndefined();
  });

  it("still gives the challenge its own link preview", () => {
    expect(content("challenge", "og:url")).toBe(`${SITE_URL}/challenge`);
    expect(content("challenge", "og:title")).toBe(pageMeta("challenge").title);
  });

  it("leaves a game's title to the game", () => {
    expect(pageMeta("game").title).toBeUndefined();
  });

  it("describes the site as schema.org data on the home page only", () => {
    const jsonLd = pageMeta("site").jsonLd as Record<string, string>[];
    expect(jsonLd.map((data) => data["@type"])).toEqual(["WebSite", "Organization"]);
    for (const data of jsonLd) {
      expect(data["@context"]).toBe("https://schema.org");
    }
    expect(pageMeta("daily").jsonLd).toBeUndefined();
  });

  it("describes a game's landing page as a free web game under the home page", () => {
    const [app, breadcrumbs] = pageMeta("TRIPOINT").jsonLd as Record<string, unknown>[];
    expect(app).toMatchObject({
      "@context": "https://schema.org",
      "@type": "WebApplication",
      name: "Tripoints",
      url: `${SITE_URL}/play/tripoints`,
      isAccessibleForFree: true,
      offers: { price: "0" },
    });
    expect(breadcrumbs).toMatchObject({
      "@type": "BreadcrumbList",
      itemListElement: [{ item: `${SITE_URL}/` }, { name: "Tripoints", item: `${SITE_URL}/play/tripoints` }],
    });
  });

  it("pitches both the daily challenge and free play for the site", () => {
    expect(content("site", "description")).toContain("daily challenge");
    expect(content("site", "description")).toContain("play freely");
  });
});
