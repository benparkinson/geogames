import { expect, test } from "../fixtures";

// Crawlers run no JavaScript, so these read the raw HTML the jar serves, with no browser.
const SITE_URL = "https://geoggernaut.com";

const PAGES = [
  {
    path: "/",
    title: "Geoggernaut: tricky geography puzzles",
    description:
      "Name countries from their shapes, borders, rivers and cities. " +
      "Take the daily challenge or play freely, as many rounds as you like.",
  },
  {
    path: "/daily",
    title: "Geoggernaut daily challenge",
    description: "Five tricky geography puzzles, the same for everyone today. How many can you solve?",
  },
  {
    path: "/challenge?id=x",
    // The apostrophe is escaped in the HTML.
    title: "You&#x27;ve been challenged on Geoggernaut",
    description: "A friend played these geography puzzles. Can you beat their score?",
  },
];

/** The `content` of every `<meta>` with this name or property. */
function metaContents(html: string, key: string): string[] {
  return (html.match(/<meta [^>]*>/g) ?? [])
    .filter((tag) => tag.includes(`name="${key}"`) || tag.includes(`property="${key}"`))
    .map((tag) => /content="([^"]*)"/.exec(tag)?.[1] ?? "");
}

for (const { path, title, description } of PAGES) {
  test(`${path} has its link preview in the HTML`, async ({ request }) => {
    const response = await request.get(path);
    expect(response.ok()).toBe(true);
    const html = await response.text();

    // Exactly one of each: a page's tags replace the site defaults.
    expect(metaContents(html, "og:title")).toEqual([title]);
    expect(metaContents(html, "og:description")).toEqual([description]);
    expect(metaContents(html, "description")).toEqual([description]);
    expect(metaContents(html, "og:image")).toEqual([`${SITE_URL}/og-image.png`]);
    expect(metaContents(html, "twitter:card")).toEqual(["summary_large_image"]);
  });
}

test("the link preview image is served", async ({ request }) => {
  const response = await request.get("/og-image.png");
  expect(response.status()).toBe(200);
  expect(response.headers()["content-type"]).toBe("image/png");
});
