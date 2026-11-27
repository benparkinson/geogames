import type { Page } from "@playwright/test";
import { answerBox, expect, startGame, test } from "../fixtures";

/** Plays a three-round game to its results page, giving up on every round. */
async function giveUpEveryRound(page: Page): Promise<void> {
  for (let round = 0; round < 3; round++) {
    await expect(answerBox(page)).toBeVisible();
    await page.getByRole("button", { name: "Give up", exact: true }).click();
    await page.getByRole("button", { name: "Tap again to give up" }).click();
    await page.getByRole("button", { name: round < 2 ? "Next round" : "See results" }).click();
  }
}

test("challenge a friend: finish a game, share the link, and a friend plays the same rounds", async ({
  page,
  api,
  browser,
  baseURL,
}) => {
  await page.context().grantPermissions(["clipboard-read", "clipboard-write"]);

  // 1. Play a three-round game, giving up on every round.
  const sourceId = await startGame(page, "Tripoints", 3);
  await giveUpEveryRound(page);

  // 2. The summary copies a spoiler-free text with a challenge link.
  await page.getByRole("button", { name: "Challenge a friend" }).click();
  await expect(page.getByRole("button", { name: "Copied" })).toBeVisible();
  const copied = await page.evaluate(() => navigator.clipboard.readText());
  expect(copied).toContain("can you beat my score?");
  expect(copied).toContain("🟥");
  expect(copied).not.toContain("/games/");
  const link = new URL(copied.split("\n").find((line) => line.includes("/challenge?id="))!);
  expect(link.origin).toBe("https://geoggernaut.com");

  // 3. A friend opens the link (on this server, not the live site): nothing starts until Play, and each Play is a
  // new game.
  const friend = await browser.newContext({ baseURL });
  const friendPage = await friend.newPage();
  const gameIds: string[] = [];
  for (let i = 0; i < 2; i++) {
    await friendPage.goto(link.pathname + link.search);
    await expect(friendPage.getByText(/The score to beat is 0 \/ 30/)).toBeVisible();
    await friendPage.getByRole("button", { name: "Play" }).click();
    await expect(friendPage).toHaveURL(/\/games\/[0-9a-f-]{36}$/);
    gameIds.push(friendPage.url().split("/").pop()!);
  }
  expect(gameIds[0]).not.toBe(gameIds[1]);
  expect(gameIds[0]).not.toBe(sourceId);

  // 4. The copy has the same round as the original.
  const original = await api.round(sourceId, 0);
  const copy = await api.round(gameIds[0], 0);
  expect(copy).toEqual(original);

  // 5. The friend finishes, takes second place (the earlier entry wins a tie), enters initials and replies with
  // the same link.
  await friend.grantPermissions(["clipboard-read", "clipboard-write"]);
  await giveUpEveryRound(friendPage);
  const table = friendPage.getByRole("list", { name: "League table" });
  await expect(table.getByRole("listitem")).toHaveCount(2);
  await friendPage.getByRole("textbox", { name: "Your initials" }).fill("abc");
  await friendPage.getByRole("button", { name: "Save" }).click();
  await expect(table.getByRole("listitem").nth(1)).toContainText("ABC");
  await expect(friendPage.getByRole("button", { name: "Pass it on" })).toBeVisible();
  await friendPage.getByRole("button", { name: "Reply" }).click();
  await expect(friendPage.getByRole("button", { name: "Copied" })).toBeVisible();
  const reply = await friendPage.evaluate(() => navigator.clipboard.readText());
  expect(reply).toContain("0 / 30, 2nd of 2.");
  expect(reply).toContain(link.href);
  await friend.close();
});
