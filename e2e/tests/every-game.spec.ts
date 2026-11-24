import { answerBox, expect, guess, roundResult, startGame, test } from "../fixtures";

// Every game type the backend has (GameType) with its title on the home page, plus RANDOM, the mix of all
// of them. A new game type needs a line here: the contract test already fails until it has a fixture.
const GAMES = [
  { type: "TRIPOINT", title: "Tripoints" },
  { type: "RIVERS_BY_SHAPE", title: "River Shapes" },
  { type: "RIVERS_BY_COUNTRIES", title: "River Countries" },
  { type: "COUNTRIES_BY_NEIGHBOURS", title: "Country Gaps" },
  { type: "COUNTRIES_BY_COASTLINE", title: "Coastlines" },
  { type: "COUNTRIES_BY_LAND_BORDERS", title: "Land Borders" },
  { type: "LAKES_BY_SHAPE", title: "Lake Shapes" },
  { type: "BORDERS_BY_SHAPE", title: "Border Shapes" },
  { type: "MERGED_COUNTRIES", title: "Merged Countries" },
  { type: "COUNTRIES_BY_CITIES", title: "Country Cities" },
  { type: "COUNTRIES_BY_RIVERS", title: "Country Rivers" },
  { type: "RANDOM", title: "Mixed" },
];

// The home page's smallest game is 3 rounds, so each game is played for 3 rounds (300 points).
const ROUNDS = 3;

for (const { type, title } of GAMES) {
  test(`${type}: play every round right first time`, async ({ page, api }) => {
    const id = await startGame(page, title, ROUNDS);
    await expect(page.locator(".leaflet-container")).toBeVisible();

    for (let round = 0; round < ROUNDS; round++) {
      // The answer box is only there once this round has loaded and is still being played.
      await expect(answerBox(page)).toBeVisible();
      const { answers } = await api.round(id, round);

      for (const answer of answers) {
        await guess(page, answer.name);
      }
      await expect(roundResult(page)).toHaveText(
        answers.length > 1 ? /No wrong guesses!\s*\+100/ : /First guess!\s*\+100/,
      );

      await page
        .getByRole("button", { name: round < ROUNDS - 1 ? "Next round" : "See results" })
        .click();
    }

    await expect(
      page.getByRole("heading", { name: "Perfect game!" }),
    ).toBeVisible();
    await expect(page.getByText(`/ ${ROUNDS * 100}`, { exact: true })).toBeVisible();
  });
}
