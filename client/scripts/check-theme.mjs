// Keeps the theme in one place. Fails, naming file and line, when:
//  - a .css file other than theme/tokens.css contains a colour literal;
//  - a .ts/.tsx file imports react-bootstrap or has an inline style with a colour;
//  - a className uses a Bootstrap colour, component or utility class (Bootstrap is gone).
//
// Run from client/: node scripts/check-theme.mjs (also the end of `npm run lint`).
import { readdirSync, readFileSync } from "node:fs";
import { join, relative, sep } from "node:path";
import { fileURLToPath } from "node:url";

// Leaflet styles these from JavaScript, on the relief map or the blank map, and they don't follow light/dark.
const COLOUR_LITERAL_EXCEPTIONS = ["src/map/palette.ts"];
const TOKENS_FILE = "src/theme/tokens.css";

const COLOUR = /#[0-9a-fA-F]{3,8}\b|\b(?:rgb|rgba|hsl|hsla)\(/;
const BOOTSTRAP_CLASS = new RegExp(
  "(?<![\\w-])(?:btn(?:-[\\w-]+)?|badge|card|list-group[\\w-]*|form-control|alert[\\w-]*|spinner-[\\w-]+" +
    "|text-(?:muted|success|secondary|warning|warning-emphasis|danger|body|body-secondary|primary|white|black|dark|light|info)" +
    "|bg-[\\w-]+|border-(?:primary|success|secondary|danger|warning))(?![\\w-])",
);

// Bootstrap's layout, spacing and type utilities. Bootstrap is no longer loaded, so these would do nothing.
const BREAKPOINT = "(?:sm-|md-|lg-|xl-|xxl-)?";
const BOOTSTRAP_UTILITY = new RegExp(
  "(?<![\\w-])(?:" +
    [
      `d-${BREAKPOINT}(?:none|inline|inline-block|block|grid|flex|inline-flex|table)`,
      `flex-${BREAKPOINT}(?:row|column|wrap|nowrap|fill|grow-[01]|shrink-[01])(?:-reverse)?`,
      `(?:align-items|align-self|align-content|justify-content)-${BREAKPOINT}\\w+`,
      `order-${BREAKPOINT}\\d`,
      `(?:row-|column-)?gap-${BREAKPOINT}\\d`,
      `[mp][tbsexy]?-${BREAKPOINT}(?:n?\\d|auto)`,
      "h[1-6]|display-\\d|small|lead|fw-\\w+|fs-\\d|fst-\\w+|lh-\\w+",
      "text-(?:center|start|end|nowrap|wrap|truncate|lowercase|uppercase|capitalize|decoration-\\w+)",
      "row|col(?:-(?:sm|md|lg|xl|xxl))?(?:-(?:\\d+|auto))?|g[xy]?-\\d|container(?:-\\w+)?",
      "[wh]-(?:25|50|75|100|auto)|m[wh]-100|v[wh]-100|min-v[wh]-100",
      "position-\\w+|overflow-\\w+|float-\\w+|visually-hidden(?:-focusable)?",
    ].join("|") +
    ")(?![\\w-])",
);

/** The class names written as string literals in a className value (not the JavaScript around them). */
function literalClasses(attribute) {
  const literals = attribute.match(/"[^"]*"|'[^']*'|`[^`]*`/g) ?? [];
  return literals.map((literal) => literal.slice(1, -1).replace(/\$\{[^}]*\}/g, " ")).join(" ");
}

/**
 * The Bootstrap classes used in a file's className attributes, as `{ line, problem }`. It reads the whole file, so a
 * className whose value spans several lines is checked too.
 */
export function classProblems(text) {
  const problems = [];
  const attribute = /className=(?:"[^"]*"|\{(?:[^{}]|\{[^{}]*\})*\})/g;
  for (const match of text.matchAll(attribute)) {
    const line = text.slice(0, match.index).split("\n").length;
    const classes = literalClasses(match[0]);
    const component = classes.match(BOOTSTRAP_CLASS);
    if (component) {
      problems.push({ line, problem: `Bootstrap class "${component[0]}": use src/ui or a gg- class` });
    }
    const utility = classes.match(BOOTSTRAP_UTILITY);
    if (utility) {
      problems.push({
        line,
        problem: `Bootstrap utility "${utility[0]}" does nothing (Bootstrap is gone): use a gg- class in theme/components.css`,
      });
    }
  }
  return problems;
}

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
  return (
    /\.(css|ts|tsx)$/.test(file) &&
    !/\.test\.tsx?$/.test(file) &&
    !file.endsWith(".d.ts") &&
    !file.startsWith("src/test/")
  );
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
      const text = readFileSync(path, "utf8");
      text.split("\n").forEach((line, index) => {
        for (const problem of problemsInLine(file, line)) {
          failures.push(`${file}:${index + 1}: ${problem}`);
        }
      });
      if (file.endsWith(".tsx")) {
        for (const { line, problem } of classProblems(text)) {
          failures.push(`${file}:${line}: ${problem}`);
        }
      }
    }
  }
  if (failures.length > 0) {
    console.error(failures.join("\n"));
    console.error(`\ncheck-theme: ${failures.length} problem(s).`);
    process.exit(1);
  }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  main();
}
