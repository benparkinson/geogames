import { defineConfig, globalIgnores } from "eslint/config";
import nextCoreWebVitals from "eslint-config-next/core-web-vitals";
import nextTypescript from "eslint-config-next/typescript";

export default defineConfig([
  ...nextCoreWebVitals,
  ...nextTypescript,
  {
    files: ["src/**/*.{ts,tsx}"],
    ignores: ["src/map/GameMap.tsx"],
    rules: {
      "no-restricted-imports": [
        "error",
        {
          paths: [{ name: "react-leaflet", importNames: ["MapContainer"], message: "Use GameMap, which adds the credits." }],
        },
      ],
    },
  },
  globalIgnores(["out/**", "lint-probe/**", ".next/**", "coverage/**", "node_modules/**", "next-env.d.ts"]),
]);
