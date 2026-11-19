import type { Page } from "@playwright/test";
import { answerBox, expect, test } from "../fixtures";

/** The id of the game the daily page stored for this browser. */
async function storedGameId(page: Page): Promise<string> {
  const raw = await page.evaluate(() => localStorage.getItem("gg-daily"));
  return JSON.parse(raw!).gameId;
}

test("the daily: play, share, come back, and a second player gets the same rounds", async ({
  page,
  api,
  browser,
  baseURL,
}) => {
  await page.context().grantPermissions(["clipboard-read", "clipboard-write"]);
  const dailyPosts: string[] = [];
  page.on("request", request => {
    if (request.method() === "POST" && request.url().endsWith("/api/daily-games")) {
      dailyPosts.push(request.url());
    }
  });

  // 1. Play: nothing is created until the button is clicked. Give up on every round.
  await page.goto("/daily");
  await expect(page.getByRole("heading", { name: "Daily challenge" })).toBeVisible();
  expect(dailyPosts).toHaveLength(0);
  await page.getByRole("button", { name: "Play today's challenge" }).click();
  await expect(page).toHaveURL(/\/daily$/);
  for (let round = 0; round < 5; round++) {
    await expect(answerBox(page)).toBeVisible();
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await page.getByRole("button", { name: "Reveal answer" }).click();
    await page.getByRole("button", { name: "Tap again to reveal" }).click();
    await page.getByRole("button", { name: round < 4 ? "Next round" : "See results" }).click();
  }
  expect(dailyPosts).toHaveLength(1);
  const gameId = await storedGameId(page);
  const firstRound = await api.round(gameId, 0);

  // 2. The summary shares a spoiler-free text that links to /daily.
  await expect(page.getByRole("heading", { name: /Daily \d{4}-\d{2}-\d{2}: You scored 0 \/ 500/ })).toBeVisible();
  await page.getByRole("button", { name: "Copy result" }).click();
  await expect(page.getByRole("button", { name: "Copied" })).toBeVisible();
  const copied = await page.evaluate(() => navigator.clipboard.readText());
  expect(copied).toContain("GeoGauntlet Daily 20");
  expect(copied).toContain("🟥🟥🟥🟥🟥");
  expect(copied).toContain("/daily");
  expect(copied).not.toContain("/games/");

  // 3. Reloading /daily works in the jar, shows the result, and creates no game.
  await page.reload();
  await expect(page.getByText("You scored 0 / 500")).toBeVisible();
  expect(dailyPosts).toHaveLength(1);

  // 4. Another player (empty storage) gets a game of their own with the same rounds.
  const other = await browser.newContext({ baseURL });
  try {
    const otherPage = await other.newPage();
    await otherPage.goto("/daily");
    await otherPage.getByRole("button", { name: "Play today's challenge" }).click();
    await expect(answerBox(otherPage)).toBeVisible();
    const otherId = await storedGameId(otherPage);
    expect(otherId).not.toBe(gameId);
    expect((await api.round(otherId, 0)).answers).toEqual(firstRound.answers);
  } finally {
    await other.close();
  }
});
