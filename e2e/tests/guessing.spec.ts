import { answerBox, expect, guess, startGame, test } from "../fixtures";

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
