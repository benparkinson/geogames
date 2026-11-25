import type { Page } from "@playwright/test";
import { answerBox, expect, guess, test } from "../fixtures";

const WIDTH = 360;
const HEIGHT = 640;
const KEYBOARD_HEIGHT = 352;

test.use({ viewport: { width: WIDTH, height: HEIGHT } });

async function box(locator: ReturnType<Page["locator"]>) {
  const found = await locator.boundingBox();
  expect(found).not.toBeNull();
  return found!;
}

// The newest guess row must end above the lives row (or the answer box) and be the element drawn there.
async function expectLastGuessClear(page: Page, name: string) {
  const row = page.getByRole("listitem").filter({ hasText: name }).first();
  const rowBox = await box(row);
  const status = page.getByTestId("round-status");
  const below = (await status.isVisible()) ? await box(status) : await box(answerBox(page));
  expect(rowBox.y + rowBox.height).toBeLessThanOrEqual(below.y + 1);
  const onTop = await row.evaluate(element => {
    const rect = element.getBoundingClientRect();
    const hit = document.elementFromPoint(rect.x + rect.width / 2, rect.y + rect.height / 2);
    return hit !== null && element.contains(hit);
  });
  expect(onTop).toBe(true);
}

test("on a small phone the map keeps its room, and the input and buttons stay in view", async ({ page, api }) => {
  // Fake the on-screen keyboard through the visual viewport, which GameLayout watches.
  await page.addInitScript(() => {
    const original = Object.getOwnPropertyDescriptor(VisualViewport.prototype, "height")!;
    Object.defineProperty(VisualViewport.prototype, "height", {
      get() {
        return (window as unknown as { __vvh?: number }).__vvh ?? original.get!.call(this);
      },
    });
  });
  const setKeyboard = (height: number | null) =>
    page.evaluate(h => {
      (window as unknown as { __vvh?: number | null }).__vvh = h ?? undefined;
      window.visualViewport!.dispatchEvent(new Event("resize"));
    }, height);

  await page.goto("/daily");
  await page.getByRole("button", { name: "Start" }).click();
  await expect(answerBox(page)).toBeVisible();
  const gameId = JSON.parse((await page.evaluate(() => localStorage.getItem("gg-daily")))!).gameId;
  const round = await api.round(gameId, 0);
  const answers = new Set(round.answers.map(answer => answer.name));
  const wrong = (await api.countries()).filter(country => !answers.has(country.name)).slice(0, 3);

  for (const country of wrong) {
    await guess(page, country.name);
    await expect(page.getByRole("listitem").filter({ hasText: country.name }).first()).toBeVisible();
  }
  const hint = page.getByRole("button", { name: /^Hint/ });
  for (let taken = 0; taken < 2 && (await hint.isEnabled()); taken++) {
    await hint.click();
  }

  const lastWrong = wrong[wrong.length - 1].name;
  await expectLastGuessClear(page, lastWrong);

  const map = page.locator("main");
  expect((await box(map)).height).toBeGreaterThanOrEqual(0.45 * HEIGHT - 1);
  for (const target of [answerBox(page), page.getByRole("button", { name: "Guess", exact: true })]) {
    const found = await box(target);
    expect(found.y).toBeGreaterThanOrEqual(0);
    expect(found.y + found.height).toBeLessThanOrEqual(HEIGHT);
    expect(found.x + found.width).toBeLessThanOrEqual(WIDTH);
  }

  await setKeyboard(KEYBOARD_HEIGHT);
  await expect(page.getByRole("navigation", { name: "Rounds" })).toBeHidden();
  expect((await box(map)).height).toBeGreaterThanOrEqual(0.35 * KEYBOARD_HEIGHT - 1);
  const input = await box(answerBox(page));
  expect(input.y).toBeGreaterThanOrEqual(0);
  expect(input.y + input.height).toBeLessThanOrEqual(KEYBOARD_HEIGHT);
  await expectLastGuessClear(page, lastWrong);
  await setKeyboard(null);

  for (const name of answers) {
    await guess(page, name);
  }
  const next = page.getByRole("button", { name: /Next round/ });
  await expect(next).toBeVisible();
  const found = await box(next);
  expect(found.y + found.height).toBeLessThanOrEqual(HEIGHT);
});
