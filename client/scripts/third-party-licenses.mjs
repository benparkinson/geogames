// Writes public/third-party-licenses.txt: the licence text of every npm package that can end up in
// the site (the production dependencies in package-lock.json, not dev or optional ones). Their MIT,
// BSD, ISC and Hippocratic licences require the copyright notice and licence to travel with every
// copy, and the minified bundle keeps no comments, so the footer links to this file instead.
// Run by `npm run build` and `npm run dev`; the output is generated, so it is git-ignored.
import { existsSync, readdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const lock = JSON.parse(readFileSync(join(root, "package-lock.json"), "utf8"));
const LICENSE_FILE = /^(licen[cs]e|copying|notice)(\.(md|txt|markdown))?$/i;

const entries = Object.entries(lock.packages)
  .filter(([path, info]) => path.startsWith("node_modules/") && !info.dev && !info.optional && !info.devOptional)
  .map(([path, info]) => {
    const name = path.slice(path.lastIndexOf("node_modules/") + "node_modules/".length);
    return { path, name, version: info.version, license: info.license ?? "UNKNOWN" };
  })
  .sort((a, b) => a.name.localeCompare(b.name) || a.version.localeCompare(b.version));

const seen = new Set();
const sections = [];
for (const { path, name, version, license } of entries) {
  const key = `${name}@${version}`;
  if (seen.has(key)) continue;
  seen.add(key);
  const dir = join(root, path);
  const files = existsSync(dir) ? readdirSync(dir).filter((f) => LICENSE_FILE.test(f)).sort() : [];
  const texts = files.map((f) => readFileSync(join(dir, f), "utf8").trim());
  const body = texts.length > 0 ? texts.join("\n\n") : `Licence: ${license} (no licence file in the package)`;
  sections.push(`${key} (${license})\n${"-".repeat(key.length + license.length + 3)}\n\n${body}\n`);
}

const header = [
  "Third-party software used in Geogames",
  "=====================================",
  "",
  "Geogames is built with the open-source packages below. Each is listed with its licence text.",
  "Data credits (Natural Earth, Wikidata, Wikipedia, Esri, Flaticon) are on the home page.",
  "",
  "",
].join("\n");

const out = join(root, "public", "third-party-licenses.txt");
writeFileSync(out, header + sections.join("\n\n"), "utf8");
console.log(`Wrote ${sections.length} package licences to public/third-party-licenses.txt`);
