import { expect, test } from "../fixtures";

const TEXT = "A new version of Geogames is available.";

test("a newer deployed version shows a toast, and Refresh clears it", async ({ page }) => {
  const newer = (route: import("@playwright/test").Route) =>
    route.fulfill({ json: { buildId: "something-else" } });
  await page.route("**/version.json*", newer);
  await page.goto("/");

  await expect(page.getByRole("status").filter({ hasText: TEXT })).toBeVisible();

  // The real version.json matches this build, so the reloaded page needs no notice.
  await page.unroute("**/version.json*", newer);
  const loaded = page.waitForEvent("load");
  const checked = page.waitForResponse(response => response.url().includes("/version.json"));
  await page.getByRole("button", { name: "Refresh" }).click();
  await loaded;
  await checked;
  await expect(page.getByRole("status").filter({ hasText: TEXT })).toHaveCount(0);
});

test("no toast when the deployed version is this one", async ({ page }) => {
  const checked = page.waitForResponse(response => response.url().includes("/version.json"));
  await page.goto("/");
  await checked;
  await expect(page.getByRole("status").filter({ hasText: TEXT })).toHaveCount(0);
});
