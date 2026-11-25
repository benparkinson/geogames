import { expect, test } from "../fixtures";

// These mirror GAME_MODULES in client/src/games/registry.ts, in order, followed by the Mixed (RANDOM)
// option. Adding a game there means adding its title here.
const GAME_TITLES = [
  "Tripoints",
  "City Map",
  "Merged Countries",
  "Missing Country",
  "Follow the River",
  "Land Borders",
  "Waterways",
  "Coastlines",
  "River Shapes",
  "Lake Shapes",
  "Border Shapes",
  "Mixed",
];

test("the home page offers every game and the round counts", async ({ page }) => {
  await page.goto("/");

  await expect(page.getByRole("heading", { name: "Geoggernaut" })).toBeVisible();
  const games = page.getByRole("group", { name: "Choose a game" }).getByRole("button");
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

for (const theme of ["light", "dark"] as const) {
  test(`the game cards' titles and icons follow the ${theme} theme`, async ({ page }) => {
    await page.addInitScript((saved) => localStorage.setItem("gg-theme", saved), theme);
    await page.goto("/");
    await expect(page.locator("html")).toHaveAttribute("data-theme", theme);

    // The cards are buttons, whose browser default colour ignores the theme.
    const text = await page.locator("body").evaluate((body) => getComputedStyle(body).color);
    const card = page
      .getByRole("button")
      .filter({ has: page.getByText("Tripoints", { exact: true }) });
    await expect(card.getByText("Tripoints", { exact: true })).toHaveCSS("color", text);
    await expect(card.locator("svg").first()).toHaveCSS("color", text);
  });
}

test("the privacy page opens from the footer and loads directly", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("contentinfo").getByRole("link", { name: "Credits and privacy" }).click();
  await expect(page.getByRole("heading", { name: "Credits and privacy", level: 1 })).toBeVisible();

  await page.goto("/privacy");
  await expect(page.getByRole("heading", { name: "Credits and privacy", level: 1 })).toBeVisible();
});

test.describe("on a phone", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test("the start button is on screen after choosing a game", async ({ page }) => {
    await page.goto("/");
    await page
      .getByRole("button")
      .filter({ has: page.getByText("Lake Shapes", { exact: true }) })
      .click();

    const startButton = page.getByRole("button", { name: "Start game" });
    await expect(startButton).toBeInViewport();
  });
});

test.describe("on a wide screen", () => {
  test.use({ viewport: { width: 1440, height: 900 } });

  test("the start panel sits directly under the chosen row", async ({ page }) => {
    await page.goto("/");
    const row = page
      .getByRole("button")
      .filter({ has: page.getByText("Tripoints", { exact: true }) });
    await row.click();

    await expect(page.getByRole("button", { name: "Start game" })).toBeInViewport();
    const rowBox = await row.boundingBox();
    const panel = await page.getByRole("complementary", { name: "Start a game" }).boundingBox();
    const group = await page.getByRole("group", { name: "Choose a game" }).boundingBox();
    expect(panel!.y).toBeGreaterThanOrEqual(rowBox!.y + rowBox!.height - 1);
    expect(panel!.x).toBeGreaterThanOrEqual(group!.x);
    expect(panel!.x + panel!.width).toBeLessThanOrEqual(group!.x + group!.width + 1);

    // Choosing another game moves the panel to it.
    await page
      .getByRole("button")
      .filter({ has: page.getByText("Lake Shapes", { exact: true }) })
      .click();
    await expect(page.getByRole("button", { name: "Start game" })).toHaveCount(1);
  });
});

test("the Settings popup picks the theme and System default follows the device", async ({
  page,
}) => {
  await page.emulateMedia({ colorScheme: "light" });
  await page.goto("/");
  // The radios are hidden; click the visible labels.
  await page.getByRole("button", { name: "Settings" }).click();
  await expect(page.getByRole("radio", { name: "System default" })).toBeChecked();

  await page.getByText("Dark", { exact: true }).click();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  await page.reload();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");

  await page.getByRole("button", { name: "Settings" }).click();
  await page.getByText("System default", { exact: true }).click();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "light");
  expect(await page.evaluate(() => localStorage.getItem("gg-theme"))).toBeNull();
});
