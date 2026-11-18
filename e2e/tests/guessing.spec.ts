import { answerBox, expect, guess, reloadGamePage, startGame, test, type Api } from "../fixtures";

test("an unknown name is rejected without using a guess, a wrong country uses one", async ({ page, api }) => {
  const countriesLoaded = page.waitForResponse(response => response.url().endsWith("/api/countries"));
  const id = await startGame(page, "Tripoints", 3);
  await countriesLoaded;
  await expect(answerBox(page)).toBeVisible();
  await expect(page.getByRole("status", { name: "5 guesses left" })).toBeVisible();

  await guess(page, "Narnia");
  await expect(page.getByText(`We don't recognise "Narnia" as a country. Check the spelling.`)).toBeVisible();
  await expect(page.getByRole("status", { name: "5 guesses left" })).toBeVisible();

  const { answers } = await api.round(id, 0);
  const answerNames = new Set(answers.flatMap(a => [a.name, ...a.additionalNames]).map(n => n.toLowerCase()));
  const wrong = (await api.countries()).map(c => c.name).find(name => /^[A-Za-z ]+$/.test(name) && !answerNames.has(name.toLowerCase()));
  await guess(page, wrong!);
  await expect(page.getByText(`"${wrong}" isn't right. 4 guesses left.`)).toBeVisible();
  await expect(page.getByRole("status", { name: "4 guesses left" })).toBeVisible();
  await expect(page.getByText("Wrong guesses")).toBeVisible();
});

test("an alias of an answer is accepted as that answer", async ({ page, api }) => {
  // Not every tripoint has a country with an alias (such as Holland for the Netherlands), so start
  // games until one does. A cap keeps a data change from looping forever.
  const MAX_GAMES = 25;
  let found: { id: string; canonical: string; alias: string } | undefined;
  for (let attempt = 0; attempt < MAX_GAMES && !found; attempt++) {
    const id = await startGame(page, "Tripoints", 3);
    const { answers } = await api.round(id, 0);
    const plain = (alias: string) => /^[A-Za-z ]+$/.test(alias);
    const withAlias = answers.find(answer => answer.additionalNames.some(plain));
    if (withAlias) {
      found = { id, canonical: withAlias.name, alias: withAlias.additionalNames.find(plain)! };
    }
  }
  expect(found, `no tripoint with an alias in ${MAX_GAMES} games`).toBeDefined();

  await expect(answerBox(page)).toBeVisible();
  await guess(page, found!.alias);
  await expect(page.getByText(/^Correct! 2 to go\.$/)).toBeVisible();
  await expect(page.getByRole("group", { name: "Answers: 1 of 3 found" })).toContainText(found!.canonical);
  await expect(page.getByRole("status", { name: "5 guesses left" })).toBeVisible();
});

/** A country that is not the answer, and plain enough to type. */
async function wrongCountry(api: Api, id: string): Promise<string> {
  const { answers } = await api.round(id, 0);
  const answerNames = new Set(answers.flatMap(a => [a.name, ...a.additionalNames]).map(n => n.toLowerCase()));
  const countries = await api.countries();
  return countries.map(c => c.name).find(name => /^[A-Za-z ]+$/.test(name) && !answerNames.has(name.toLowerCase()))!;
}

test("a wrong country in a country game says how far and which way the answer is", async ({ page, api }) => {
  const id = await startGame(page, "Country Gaps", 3);
  await expect(answerBox(page)).toBeVisible();

  await guess(page, await wrongCountry(api, id));

  await expect(
    page.getByText(/isn't right\. The answer is [\d,]+ km to the (north|south|east|west)/),
  ).toBeVisible();
});

test("Hardcore mode, switched on from the home page, hides the distance clues", async ({ page, api }) => {
  await page.goto("/");
  await page.getByRole("radio").filter({ has: page.getByText("Country Gaps", { exact: true }) }).click();
  await page.getByRole("group", { name: "Hardcore mode" }).getByText("On", { exact: true }).click();

  const id = await startGame(page, "Country Gaps", 3);
  await expect(answerBox(page)).toBeVisible();
  const wrong = await wrongCountry(api, id);
  await guess(page, wrong);

  await expect(page.getByText(`"${wrong}" isn't right. 4 guesses left.`)).toBeVisible();
  await expect(page.locator(".guess-feedback")).not.toContainText("km");
});

test("a refresh keeps the wrong guesses, and a repeat of one is still a duplicate", async ({ page, api }) => {
  const countriesLoaded = page.waitForResponse(response => response.url().endsWith("/api/countries"));
  const id = await startGame(page, "Tripoints", 3);
  await countriesLoaded;
  await expect(answerBox(page)).toBeVisible();

  const { answers } = await api.round(id, 0);
  const answerNames = new Set(answers.flatMap(a => [a.name, ...a.additionalNames]).map(n => n.toLowerCase()));
  const wrong = (await api.countries()).map(c => c.name).find(name => /^[A-Za-z ]+$/.test(name) && !answerNames.has(name.toLowerCase()));
  const saved = page.waitForResponse(response => response.url().endsWith("/guesses") && response.ok());
  await guess(page, wrong!);
  await expect(page.getByRole("status", { name: "4 guesses left" })).toBeVisible();
  await saved;

  await reloadGamePage(page, id);

  await expect(answerBox(page)).toBeVisible();
  await expect(page.getByRole("status", { name: "4 guesses left" })).toBeVisible();
  await expect(page.getByText("Wrong guesses")).toBeVisible();
  await expect(page.getByText(wrong!, { exact: true })).toBeVisible();

  await guess(page, wrong!);
  await expect(page.getByText(`You've already guessed ${wrong}.`)).toBeVisible();
  await expect(page.getByRole("status", { name: "4 guesses left" })).toBeVisible();
});
