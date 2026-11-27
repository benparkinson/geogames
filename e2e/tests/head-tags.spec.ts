import { expect, test } from "../fixtures";

// Crawlers run no JavaScript, so these read the raw HTML the jar serves, with no browser.
const SITE_URL = "https://geoggernaut.com";

const PAGES = [
  {
    path: "/",
    title: "Geoggernaut: tricky geography puzzles and map quizzes",
    canonical: `${SITE_URL}/`,
    description:
      "Name countries from their shapes, borders, rivers and cities. " +
      "Take the daily challenge or play freely, as many rounds as you like.",
  },
  {
    path: "/daily",
    title: "Daily geography challenge | Geoggernaut",
    canonical: `${SITE_URL}/daily`,
    description:
      "Five tricky geography puzzles, the same for everyone today. How many can you solve?",
  },
  {
    path: "/challenge?id=x",
    // The apostrophe is escaped in the HTML.
    canonical: null,
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

/** The `href` of every canonical link. */
function canonicals(html: string): string[] {
  return (html.match(/<link [^>]*rel="canonical"[^>]*>/g) ?? []).map(
    (tag) => /href="([^"]*)"/.exec(tag)?.[1] ?? "",
  );
}

for (const { path, title, description, canonical } of PAGES) {
  test(`${path} has its head tags in the HTML`, async ({ request }) => {
    const response = await request.get(path);
    expect(response.ok()).toBe(true);
    const html = await response.text();

    // Exactly one of each: a page's tags replace the site defaults.
    expect([...html.matchAll(/<title[^>]*>([^<]*)<\/title>/g)].map((match) => match[1])).toEqual([
      title,
    ]);
    expect(canonicals(html)).toEqual(canonical ? [canonical] : []);
    expect(metaContents(html, "robots")).toEqual(canonical ? [] : ["noindex"]);
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

test("the home page has structured data that parses", async ({ request }) => {
  const html = await (await request.get("/")).text();
  const scripts = [...html.matchAll(/<script type="application\/ld\+json"[^>]*>(.*?)<\/script>/g)];
  expect(scripts.map((match) => JSON.parse(match[1])["@type"])).toEqual([
    "WebSite",
    "Organization",
  ]);
});

test("a game's landing page is in the HTML, with its canonical link and structured data", async ({
  request,
}) => {
  const response = await request.get("/play/tripoints");
  expect(response.status()).toBe(200);
  const html = await response.text();

  expect(html).toContain("Tripoints: name the three countries that meet at a point</h1>");
  expect(canonicals(html)).toEqual([`${SITE_URL}/play/tripoints`]);
  const scripts = [...html.matchAll(/<script type="application\/ld\+json"[^>]*>(.*?)<\/script>/g)];
  expect(scripts.map((match) => JSON.parse(match[1])["@type"])).toEqual([
    "WebApplication",
    "BreadcrumbList",
  ]);
});

test("a game starts from its landing page", async ({ page }) => {
  await page.goto("/play/tripoints");
  await page.getByRole("button", { name: "Start game" }).click();
  await expect(page).toHaveURL(/\/games\/[0-9a-f-]+$/);
});

for (const path of ["/robots.txt", "/sitemap.xml", "/llms.txt"]) {
  test(`${path} is served`, async ({ request }) => {
    expect((await request.get(path)).status()).toBe(200);
  });
}
