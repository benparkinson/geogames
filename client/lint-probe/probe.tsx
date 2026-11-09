// Deliberately broken code for scripts/check-lint-rules.mjs. Every line below breaks one rule
// from a different ESLint plugin, so if an ESLint or plugin upgrade silently turns a plugin off,
// the check fails. `npm run lint` ignores this folder (eslint.config.mjs) and tsc excludes it.
import { useState } from "react";

export function Probe({ items, flag }: { items: string[]; flag: boolean }) {
  if (flag) {
    useState(0); // react-hooks/rules-of-hooks
  }
  var unused = 1; // no-var (core), @typescript-eslint/no-unused-vars
  return (
    <div>
      <img src="/x.png" /> {/* jsx-a11y/alt-text, @next/next/no-img-element */}
      {items.map((item) => (
        <span>{item}</span> // react/jsx-key
      ))}
    </div>
  );
}

export default { probe: Probe }; // import/no-anonymous-default-export
