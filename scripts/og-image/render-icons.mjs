// Draws the app icons in client/public/ from icons.html. Not part of the build: the PNGs are committed.
// Draw them from the SVG at each size; never scale up logo.png. After `npm ci` in e2e/ (Playwright's
// Chromium), run from the repo root: target/node/node scripts/og-image/render-icons.mjs
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const { chromium } = createRequire(new URL("../../e2e/package.json", import.meta.url))("@playwright/test");

const ICONS = [
  { file: "icon-192.png", size: 192 },
  { file: "icon-512.png", size: 512 },
  { file: "icon-maskable-512.png", size: 512, variant: "maskable" },
  { file: "apple-touch-icon.png", size: 180 },
];

const browser = await chromium.launch();
try {
  const page = await browser.newPage();
  for (const { file, size, variant = "plain" } of ICONS) {
    await page.setViewportSize({ width: size, height: size });
    const source = new URL("icons.html", import.meta.url);
    source.search = new URLSearchParams({ size, variant }).toString();
    await page.goto(source.href);
    const output = fileURLToPath(new URL(`../../client/public/${file}`, import.meta.url));
    await page.screenshot({ path: output });
    console.log(output);
  }
} finally {
  await browser.close();
}
