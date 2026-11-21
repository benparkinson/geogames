import { answerBox, expect, startGame, test } from "../fixtures";

test("the guess box suggests names, and tapping one fills the box and keeps the focus", async ({ page }, testInfo) => {
  const countriesLoaded = page.waitForResponse(response => response.url().endsWith("/api/countries"));
  await startGame(page, "Tripoints", 3);
  await countriesLoaded;
  const box = answerBox(page);
  await expect(box).toBeVisible();

  await box.click();
  await box.fill("Fra");
  const france = page.getByRole("option", { name: "France" });
  await expect(france).toBeVisible();

  const list = await page.getByRole("listbox").boundingBox();
  const input = await box.boundingBox();
  if (testInfo.project.name === "mobile") {
    // The list opens upwards over the map, ending above the box.
    expect(list!.y + list!.height).toBeLessThanOrEqual(input!.y);
  } else {
    // The side panel has room below, so the list opens downwards.
    expect(list!.y).toBeGreaterThanOrEqual(input!.y + input!.height);
  }

  await france.click();
  await expect(box).toHaveValue("France");
  await expect(box).toBeFocused();
  await expect(page.getByRole("listbox")).toHaveCount(0);
});
