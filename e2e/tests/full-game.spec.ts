import type { Page } from "@playwright/test";
import { answerBox, expect, guess, startGame, test } from "../fixtures";

/**
 * Starts listening for the next answer the page posts. Call it before the action that ends the
 * round, so the request isn't missed, and await the result afterwards.
 */
function nextAnswerPosted(page: Page) {
  return page.waitForRequest(request => request.method() === "POST" && /\/rounds\/\d+\/answers$/.test(request.url()));
}

// The numbers come from model/Scoring: a correct round is worth 100, hints share a 50-point penalty
// between the type's hints (1 of Tripoint's 3 costs 17, so 83), a revealed round and a round that ran
// out of guesses (5 for Tripoint) are worth 0.
test("a Tripoint game with a hint, a reveal and running out of guesses", async ({ page, api }) => {
  const id = await startGame(page, "Tripoints", 3);

  // Round 1: one hint, then the right answers.
  await expect(answerBox(page)).toBeVisible();
  const first = await api.round(id, 0);
  await page.getByRole("button", { name: "Hint (3 left)" }).click();
  await expect(page.getByRole("button", { name: "Hint (2 left)" })).toBeVisible();
  await guess(page, first.answers[0].name);
  const firstPosted = nextAnswerPosted(page);
  for (const answer of first.answers.slice(1)) {
    await guess(page, answer.name);
  }
  expect((await firstPosted).postDataJSON()).toEqual({ answerState: "CORRECT", hintsUsed: 1, wrongGuesses: 0 });
  await expect(page.getByText("Correct! +83")).toBeVisible();
  await expect(page.getByText("1 hint used")).toBeVisible();
  await page.getByRole("button", { name: "Next round" }).click();

  // Round 2: give up. The button asks for a second click.
  await expect(answerBox(page)).toBeVisible();
  const second = await api.round(id, 1);
  await page.getByRole("button", { name: "Reveal answer" }).click();
  const secondPosted = nextAnswerPosted(page);
  await page.getByRole("button", { name: "Tap again to reveal" }).click();
  expect((await secondPosted).postDataJSON()).toEqual({ answerState: "GAVE_UP", hintsUsed: 0, wrongGuesses: 0 });
  await expect(page.getByText("Answer revealed · 0 pts")).toBeVisible();
  for (const answer of second.answers) {
    await expect(page.getByRole("group", { name: /Answers:/ })).toContainText(answer.name);
  }
  await page.getByRole("button", { name: "Next round" }).click();

  // Round 3: wrong guesses of real countries until the limit ends the round.
  await expect(answerBox(page)).toBeVisible();
  const third = await api.round(id, 2);
  const answerNames = new Set(third.answers.flatMap(a => [a.name, ...a.additionalNames]).map(n => n.toLowerCase()));
  const wrong = (await api.countries())
    .map(country => country.name)
    .filter(name => /^[A-Za-z ]+$/.test(name) && !answerNames.has(name.toLowerCase()))
    .slice(0, 5);
  // 5 is GameType.TRIPOINT's guess limit, and what the round reports as its wrong guesses.
  expect(third.guessLimit).toBe(5);
  expect(wrong).toHaveLength(5);
  let thirdPosted: ReturnType<typeof nextAnswerPosted> | undefined;
  for (const [i, name] of wrong.entries()) {
    if (i === 4) {
      thirdPosted = nextAnswerPosted(page);
    }
    await guess(page, name);
    if (i < 4) {
      await expect(page.getByRole("status", { name: i === 3 ? "1 guess left" : `${4 - i} guesses left` })).toBeVisible();
    }
  }
  expect((await thirdPosted!).postDataJSON()).toEqual({
    answerState: "OUT_OF_GUESSES",
    hintsUsed: 0,
    wrongGuesses: third.guessLimit,
  });
  await expect(page.getByText("Out of guesses · 0 pts")).toBeVisible();
  await page.getByRole("button", { name: "See results" }).click();

  // The summary.
  await expect(page.getByRole("heading", { name: "You scored 83 / 300" })).toBeVisible();
  await expect(page.getByText("1 with hints · 2 revealed")).toBeVisible();
  const rounds = page.locator(".gg-list__item");
  await expect(rounds).toHaveCount(3);
  await expect(rounds.nth(0)).toContainText("1 hint");
  await expect(rounds.nth(0)).toContainText("+83");
  await expect(rounds.nth(1)).toContainText("+0");
  await expect(rounds.nth(2)).toContainText("Out of guesses");
  await expect(rounds.nth(2)).toContainText("+0");
});
