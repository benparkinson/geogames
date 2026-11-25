import { answerBox, expect, guess, reloadGamePage, roundResult, startGame, test } from "../fixtures";

// The numbers come from model/Scoring: each hint costs 1 point, so a correct round with one hint is worth 9.
test("a refresh keeps a hint taken, and its cost", async ({ page, api }) => {
  const id = await startGame(page, "Tripoints", 3);
  await expect(answerBox(page)).toBeVisible();
  const { answers } = await api.round(id, 0);

  const saved = page.waitForResponse(
    (response) => response.url().endsWith("/hints") && response.ok(),
  );
  await page.getByRole("button", { name: /Hint .*· 3 left/ }).click();
  await expect(page.getByRole("button", { name: /Hint .*· 2 left/ })).toBeVisible();
  await saved;

  await reloadGamePage(page, id);

  await expect(answerBox(page)).toBeVisible();
  await expect(page.getByRole("button", { name: /Hint .*· 2 left/ })).toBeVisible();
  await expect(page.getByText("Worth 9", { exact: true })).toBeVisible();

  for (const answer of answers) {
    await guess(page, answer.name);
  }
  await expect(roundResult(page)).toHaveText(/Correct with a hint\s*\+9/);
});
