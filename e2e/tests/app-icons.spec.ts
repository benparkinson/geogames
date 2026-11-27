import { expect, test } from "../fixtures";

test("every page links the app manifest and the touch icon, and the jar serves them", async ({ request }) => {
  const html = await (await request.get("/")).text();
  expect(html).toMatch(/<link rel="manifest" href="\/manifest.json"/);
  expect(html).toMatch(/<link rel="apple-touch-icon" href="\/apple-touch-icon.png"/);

  expect((await request.get("/manifest.json")).status()).toBe(200);
  for (const path of ["/icon-512.png", "/apple-touch-icon.png"]) {
    const response = await request.get(path);
    expect(response.status(), path).toBe(200);
    expect(response.headers()["content-type"], path).toBe("image/png");
  }
});
