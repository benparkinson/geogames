import type { Locator, Page } from "@playwright/test";
import { answerBox, expect, guess, startGame, test } from "../fixtures";

const WIDTH = 360;
const HEIGHT = 640;
const KEYBOARD_HEIGHT = 352;

// Every game type, with the list its guesses come from. Games are started from the home page, never as the daily,
// so a run doesn't depend on its date. The rounds are random, so the layout has to hold for any of them.
const GAMES = [
  { type: "TRIPOINT", title: "Tripoints", list: "countries" },
  { type: "RIVERS_BY_SHAPE", title: "River Shapes", list: "rivers" },
  { type: "RIVERS_BY_COUNTRIES", title: "Follow the River", list: "rivers" },
  { type: "COUNTRIES_BY_NEIGHBOURS", title: "Missing Country", list: "countries" },
  { type: "COUNTRIES_BY_COASTLINE", title: "Coastlines", list: "countries" },
  { type: "COUNTRIES_BY_LAND_BORDERS", title: "Land Borders", list: "countries" },
  { type: "LAKES_BY_SHAPE", title: "Lake Shapes", list: "lakes" },
  { type: "BORDERS_BY_SHAPE", title: "Border Shapes", list: "countries" },
  { type: "MERGED_COUNTRIES", title: "Merged Countries", list: "countries" },
  { type: "COUNTRIES_BY_CITIES", title: "City Map", list: "countries" },
  { type: "COUNTRIES_BY_RIVERS", title: "Waterways", list: "countries" },
] as const;

test.use({ viewport: { width: WIDTH, height: HEIGHT } });

async function box(locator: Locator) {
  const found = await locator.boundingBox();
  expect(found).not.toBeNull();
  return found!;
}

function guessRow(page: Page, name: string) {
  return page.getByRole("listitem").filter({ has: page.getByText(name, { exact: true }) });
}

// The newest guess row must end above the lives row and be the element drawn there. Polled: the list scrolls
// after it has been resized.
async function expectLastGuessClear(page: Page, name: string) {
  const row = guessRow(page, name);
  await expect(async () => {
    const rowBox = await box(row);
    const status = await box(page.getByTestId("round-status"));
    expect(rowBox.y + rowBox.height).toBeLessThanOrEqual(status.y + 1);
    const onTop = await row.evaluate(element => {
      const rect = element.getBoundingClientRect();
      const hit = document.elementFromPoint(rect.x + rect.width / 2, rect.y + rect.height / 2);
      return hit !== null && element.contains(hit);
    });
    expect(onTop).toBe(true);
  }).toPass({ timeout: 5_000 });
}

// Fakes the on-screen keyboard through the visual viewport, which GameLayout watches.
async function fakeKeyboard(page: Page) {
  await page.addInitScript(() => {
    const original = Object.getOwnPropertyDescriptor(VisualViewport.prototype, "height")!;
    Object.defineProperty(VisualViewport.prototype, "height", {
      get() {
        return (window as unknown as { __vvh?: number }).__vvh ?? original.get!.call(this);
      },
    });
  });
  return (height: number | null) =>
    page.evaluate(h => {
      (window as unknown as { __vvh?: number }).__vvh = h ?? undefined;
      window.visualViewport!.dispatchEvent(new Event("resize"));
    }, height);
}

for (const { type, title, list } of GAMES) {
  test(`${type}: on a small phone the map keeps its room and the newest guess stays in view`, async ({
    page,
    api,
    request,
  }) => {
    const setKeyboard = await fakeKeyboard(page);
    const id = await startGame(page, title, 3);
    await expect(answerBox(page)).toBeVisible();
    const round = await api.round(id, 0);
    const answers = round.answers.map(answer => answer.name);

    // The longest names are the worst case: their rows are the widest and can wrap.
    const names: { name: string }[] = await (await request.get(`/api/${list}`)).json();
    const wrong = names
      .map(place => place.name)
      .filter(name => !answers.includes(name))
      .sort((a, b) => b.length - a.length || a.localeCompare(b))
      .slice(0, Math.min(3, round.guessLimit - 1));
    for (const name of wrong) {
      await guess(page, name);
      await expect(guessRow(page, name)).toBeVisible();
    }
    // Hints come after the guesses: each one takes room from the list that already shows them.
    const hint = page.getByRole("button", { name: /^Hint/ });
    while (await hint.isEnabled()) {
      await hint.click();
    }

    const lastWrong = wrong[wrong.length - 1];
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
    expect((await box(map)).height).toBeGreaterThanOrEqual(0.28 * KEYBOARD_HEIGHT - 1);
    const input = await box(answerBox(page));
    expect(input.y).toBeGreaterThanOrEqual(0);
    expect(input.y + input.height).toBeLessThanOrEqual(KEYBOARD_HEIGHT);
    await expect(page.getByTestId("round-status")).toBeVisible();
    await expectLastGuessClear(page, lastWrong);
    const newest = await box(guessRow(page, lastWrong));
    expect(newest.y + newest.height).toBeLessThanOrEqual(KEYBOARD_HEIGHT);
    await setKeyboard(null);

    for (const name of answers) {
      await guess(page, name);
    }
    const next = page.getByRole("button", { name: /Next round/ });
    await expect(next).toBeVisible();
    const found = await box(next);
    // Within a pixel: the panel's fractional heights can put the button's edge a fraction below the screen.
    expect(found.y + found.height).toBeLessThanOrEqual(HEIGHT + 1);
  });
}

test("on a small phone short wrong guesses sit side by side as chips", async ({ page, api, request }) => {
  // Tripoint guesses carry no distance clue, so two short names always fit on one line.
  const id = await startGame(page, "Tripoints", 3);
  await expect(answerBox(page)).toBeVisible();
  const answers = (await api.round(id, 0)).answers.map(answer => answer.name);
  const names: { name: string }[] = await (await request.get("/api/countries")).json();
  const short = names
    .map(place => place.name)
    .filter(name => !answers.includes(name))
    .sort((a, b) => a.length - b.length || a.localeCompare(b))
    .slice(0, 2);

  for (const name of short) {
    await guess(page, name);
    await expect(guessRow(page, name)).toBeVisible();
  }

  expect((await box(guessRow(page, short[0]))).y).toBe((await box(guessRow(page, short[1]))).y);
});
