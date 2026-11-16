// Keeps the theme in one place (docs/plans/theme.md). Fails, naming file and line, when:
//  - a .css file other than theme/tokens.css contains a colour literal;
//  - a .ts/.tsx file imports react-bootstrap or has an inline style with a colour;
//  - a className uses a Bootstrap colour or component class that src/ui and theme/components.css replace.
//
// Run from client/: node scripts/check-theme.mjs (also the end of `npm run lint`).
import { readdirSync, readFileSync } from "node:fs";
import { join, relative, sep } from "node:path";
import { fileURLToPath } from "node:url";

// Leaflet styles these from JavaScript, on satellite imagery or the blank map, and they don't follow light/dark.
const COLOUR_LITERAL_EXCEPTIONS = [
  "src/map/constants.ts", // answer, revealed and clue colours for Leaflet layers
  "src/map/flagIcons.ts", // flag marker SVGs
];
const TOKENS_FILE = "src/theme/tokens.css";

const COLOUR = /#[0-9a-fA-F]{3,8}\b|\b(?:rgb|rgba|hsl|hsla)\(/;
const BOOTSTRAP_CLASS = new RegExp(
  "(?<![\\w-])(?:btn(?:-[\\w-]+)?|badge|card|list-group[\\w-]*|form-control|alert[\\w-]*|spinner-[\\w-]+" +
    "|text-(?:muted|success|secondary|warning|warning-emphasis|danger|body|body-secondary|primary)" +
    "|bg-[\\w-]+|border-(?:primary|success|secondary|danger|warning))(?![\\w-])",
);

/** The problems on one line of a file. `file` is the repo-relative path with forward slashes. */
export function problemsInLine(file, line) {
  const problems = [];
  if (file.endsWith(".css")) {
    // color-mix may mix with black, white or transparent.
    const code = line.replace(/\/\*.*?\*\//g, "");
    if (file !== TOKENS_FILE && COLOUR.test(code)) {
      problems.push("colour literal: use a --gg-color-* token (src/theme/tokens.css)");
    }
    return problems;
  }
  if (/from\s+["']react-bootstrap["']/.test(line)) {
    problems.push("react-bootstrap is gone: use src/ui");
  }
  if (/style=\{\{/.test(line) && /colou?r|background|#[0-9a-fA-F]{3,8}\b|rgba?\(/i.test(line)) {
    problems.push("inline style with a colour: use a class and a token");
  }
  if (!COLOUR_LITERAL_EXCEPTIONS.includes(file) && COLOUR.test(line) && !/^\s*(\/\/|\*|\/\*)/.test(line)) {
    problems.push("colour literal: use a token");
  }
  const classNames = line.match(/className=(?:"[^"]*"|\{[^}]*\})/g) ?? [];
  for (const attribute of classNames) {
    const found = attribute.match(BOOTSTRAP_CLASS);
    if (found) {
      problems.push(`Bootstrap class "${found[0]}": use src/ui or a gg- class`);
    }
  }
  return problems;
}

function* walk(dir) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) {
      yield* walk(path);
    } else {
      yield path;
    }
  }
}

function isChecked(file) {
  return /\.(css|ts|tsx)$/.test(file) && !/\.test\.tsx?$/.test(file) && !file.endsWith(".d.ts") && !file.startsWith("src/test/");
}

function main() {
  const root = fileURLToPath(new URL("..", import.meta.url));
  const failures = [];
  for (const dir of ["src", "pages"]) {
    for (const path of walk(join(root, dir))) {
      const file = relative(root, path).split(sep).join("/");
      if (!isChecked(file)) {
        continue;
      }
      readFileSync(path, "utf8").split("\n").forEach((line, index) => {
        for (const problem of problemsInLine(file, line)) {
          failures.push(`${file}:${index + 1}: ${problem}`);
        }
      });
    }
  }
  if (failures.length > 0) {
    console.error(failures.join("\n"));
    console.error(`\ncheck-theme: ${failures.length} problem(s). See docs/plans/theme.md.`);
    process.exit(1);
  }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  main();
}
