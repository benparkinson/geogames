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

  await expect(page.getByRole("heading", { name: "Geogames" })).toBeVisible();
  const games = page.getByRole("radiogroup", { name: "Choose a game" }).getByRole("radio");
  await expect(games).toHaveCount(GAME_TITLES.length);
  for (const title of GAME_TITLES) {
    await expect(games.filter({ has: page.getByText(title, { exact: true }) })).toHaveCount(1);
  }

  const rounds = page.getByRole("group", { name: "Rounds" }).getByRole("radio");
  await expect(rounds).toHaveCount(3);
  for (const count of ["3", "5", "10"]) {
    await expect(page.getByRole("radio", { name: count, exact: true })).toBeAttached();
  }
  await expect(page.getByRole("button", { name: "Start game" })).toBeEnabled();
});
