// Draws client/public/og-image.png, the picture on link previews, from og-image.html. Not part of the
// build: the PNG is committed. After `npm ci` in client/ (the fonts) and e2e/ (Playwright's Chromium),
// run from the repo root: target/node/node scripts/og-image/render.mjs
import { statSync } from "node:fs";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const { chromium } = createRequire(new URL("../../e2e/package.json", import.meta.url))("@playwright/test");
const output = fileURLToPath(new URL("../../client/public/og-image.png", import.meta.url));

const browser = await chromium.launch();
try {
  const page = await browser.newPage({ viewport: { width: 1200, height: 630 } });
  await page.goto(new URL("og-image.html", import.meta.url).href);
  const fontLoaded = await page.evaluate(async () => {
    try {
      const faces = await document.fonts.load('800 100px "Archivo Variable"');
      await document.fonts.ready;
      return faces.length > 0;
    } catch {
      return false;
    }
  });
  if (!fontLoaded) throw new Error("Archivo didn't load: run npm ci in client/ first");
  await page.screenshot({ path: output });
} finally {
  await browser.close();
}
console.log(`${output}: ${Math.round(statSync(output).size / 1024)} KB (keep it under 300 KB)`);
