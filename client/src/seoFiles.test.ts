import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { INDEXABLE_PATHS } from "./siteMeta";
import { SITE_URL } from "./siteName";

const publicFile = (name: string): string => readFileSync(join(process.cwd(), "public", name), "utf8");

const SEARCH_BOTS = [
  "OAI-SearchBot",
  "ChatGPT-User",
  "PerplexityBot",
  "Perplexity-User",
  "Claude-SearchBot",
  "Claude-User",
];
const TRAINING_BOTS = [
  "GPTBot",
  "ClaudeBot",
  "Google-Extended",
  "CCBot",
  "Applebot-Extended",
  "Meta-ExternalAgent",
  "Bytespider",
];

const sitemapUrls = () => [...publicFile("sitemap.xml").matchAll(/<loc>([^<]*)<\/loc>/g)].map((match) => match[1]);

/** The rules robots.txt gives one user agent: a bot with its own group ignores the `*` group. */
function robotsRules(agent: string): string[] {
  const groups = publicFile("robots.txt")
    .split(/\n\s*\n/)
    .map((group) => group.split("\n").filter((line) => line !== "" && !line.startsWith("#")));
  const group = groups.find((lines) => lines.includes(`User-agent: ${agent}`));
  return (group ?? []).filter((line) => !line.startsWith("User-agent:"));
}

describe("sitemap.xml", () => {
  it("lists exactly the indexable pages, on the public site", () => {
    expect(sitemapUrls().sort()).toEqual(INDEXABLE_PATHS.map((path) => `${SITE_URL}${path}`).sort());
  });
});

describe("robots.txt", () => {
  it("points at the sitemap", () => {
    expect(publicFile("robots.txt").split("\n")).toContain(`Sitemap: ${SITE_URL}/sitemap.xml`);
  });

  it.each(["*", ...SEARCH_BOTS])("lets %s in, but not into games", (agent) => {
    expect(robotsRules(agent)).toEqual(["Allow: /", "Disallow: /games/"]);
  });

  it.each(TRAINING_BOTS)("keeps %s out", (agent) => {
    expect(robotsRules(agent)).toEqual(["Disallow: /"]);
  });
});

describe("llms.txt", () => {
  it("links only to pages in the sitemap, with absolute URLs", () => {
    const links = [...publicFile("llms.txt").matchAll(/\]\(([^)]*)\)/g)].map((match) => match[1]);
    expect(links.length).toBeGreaterThan(0);
    for (const link of links) {
      expect(sitemapUrls()).toContain(link);
    }
  });
});
