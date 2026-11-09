// Checks that ESLint still enforces a rule from each plugin we rely on. `npm run lint` passing
// only shows that nothing crashed on clean code: after an ESLint upgrade, a plugin outside its
// supported peer range can stop reporting without any error. This lints lint-probe/probe.tsx,
// which breaks one rule from each plugin, and fails unless every expected rule fires.
//
// Run from client/: npm run lint:rules
import { ESLint } from "eslint";

const PROBE = "lint-probe/probe.tsx";
const EXPECTED_RULES = [
  "no-var", // ESLint core
  "@typescript-eslint/no-unused-vars", // typescript-eslint
  "react/jsx-key", // eslint-plugin-react
  "react-hooks/rules-of-hooks", // eslint-plugin-react-hooks
  "jsx-a11y/alt-text", // eslint-plugin-jsx-a11y
  "import/no-anonymous-default-export", // eslint-plugin-import
  "@next/next/no-img-element", // @next/eslint-plugin-next
];

// ignore: false, because eslint.config.mjs ignores the probe for `npm run lint`.
const eslint = new ESLint({ ignore: false });
const [result] = await eslint.lintFiles([PROBE]);

const fatal = result.messages.filter((message) => message.fatal);
if (fatal.length > 0) {
  console.error(`ESLint could not parse ${PROBE}:`);
  fatal.forEach((message) => console.error(`  ${message.line}:${message.column} ${message.message}`));
  process.exit(1);
}

const reported = new Set(result.messages.map((message) => message.ruleId));
const missing = EXPECTED_RULES.filter((rule) => !reported.has(rule));

console.log(`ESLint ${ESLint.version} reported on ${PROBE}: ${[...reported].sort().join(", ")}`);
if (missing.length > 0) {
  console.error(
    `These rules no longer fire, so their plugin is not being enforced: ${missing.join(", ")}.\n` +
      "Check the plugin supports this ESLint version (npm ls eslint, and its peerDependencies).",
  );
  process.exit(1);
}
console.log(`All ${EXPECTED_RULES.length} expected rules fire.`);
