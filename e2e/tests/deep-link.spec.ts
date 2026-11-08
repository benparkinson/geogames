import { expect, startGame, test } from "../fixtures";

test("reloading a game page shows the game again", async ({ page }) => {
  // KNOWN BUG, so this test is expected to fail: the jar serves the static export, which has
  // games/[gameId].html, and nothing forwards /games/{id} to it. Reloading a game page (or opening
  // a link to one) is a 404. HttpServerIT.reloadingAGamePageIsA404 pins the same behaviour.
  // When the bug is fixed this test starts to pass, which Playwright reports as a failure: remove
  // the test.fail() line then. This is the only test.fail() allowed in the suite.
  test.fail();

  await startGame(page, "Tripoints", 3);
  await expect(page.locator(".leaflet-container")).toBeVisible();

  await page.reload();

  await expect(page.locator(".leaflet-container")).toBeVisible();
});
