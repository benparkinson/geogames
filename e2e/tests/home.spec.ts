import { expect, test } from "../fixtures";

// These mirror GAME_MODULES in client/src/games/registry.ts, in order, followed by the Mixed (RANDOM)
// option. Adding a game there means adding its title here.
const GAME_TITLES = [
  "Tripoints",
  "River Shapes",
  "Rivers on the Map",
  "River Ends",
  "River Countries",
  "Country Gaps",
  "Coastlines",
  "Land Borders",
  "Lake Shapes",
  "Mixed",
];

test("the home page offers every game and the round counts", async ({ page }) => {
  await page.goto("/");

  await expect(page.getByRole("heading", { name: "GeoGauntlet" })).toBeVisible();
  const games = page.getByRole("radiogroup", { name: "Choose a game" }).getByRole("radio");
  await expect(games).toHaveCount(GAME_TITLES.length);
  for (const title of GAME_TITLES) {
    await expect(games.filter({ has: page.getByText(title, { exact: true }) })).toHaveCount(1);
  }

  const startButton = page.getByRole("button", { name: "Start game" });
  await expect(startButton).toHaveCount(0);

  await games.filter({ has: page.getByText("Tripoints", { exact: true }) }).click();
  const rounds = page.getByRole("group", { name: "Rounds" }).getByRole("radio");
  await expect(rounds).toHaveCount(3);
  for (const count of ["3", "5", "10"]) {
    await expect(page.getByRole("radio", { name: count, exact: true })).toBeAttached();
  }
  await expect(startButton).toBeEnabled();
});

test.describe("on a phone", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test("the start button is on screen after choosing a game", async ({ page }) => {
    await page.goto("/");
    await page.getByRole("radio").filter({ has: page.getByText("Lake Shapes", { exact: true }) }).click();

    const startButton = page.getByRole("button", { name: "Start game" });
    await expect(startButton).toBeInViewport();

    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await expect(page.getByRole("link", { name: "Software licences" })).toBeInViewport();
    await expect(startButton).toBeInViewport();
  });
});

test.describe("on a wide screen", () => {
  test.use({ viewport: { width: 1440, height: 900 } });

  test("the start button sits beside the cards", async ({ page }) => {
    await page.goto("/");
    await page.getByRole("radio").filter({ has: page.getByText("Tripoints", { exact: true }) }).click();

    const startButton = page.getByRole("button", { name: "Start game" });
    await expect(startButton).toBeInViewport();
    const panel = await page.getByRole("complementary", { name: "Start a game" }).boundingBox();
    const group = await page.getByRole("radiogroup", { name: "Choose a game" }).boundingBox();
    expect(panel!.x).toBeGreaterThanOrEqual(group!.x + group!.width);

    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await page.getByRole("radio").filter({ has: page.getByText("Lake Shapes", { exact: true }) }).click();
    await expect(startButton).toBeInViewport();
  });
});
