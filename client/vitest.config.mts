import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
    coverage: {
      provider: "v8",
      reporter: ["text", "html", "lcov"],
      thresholds: { statements: 99, branches: 98, functions: 99, lines: 99 },
      include: ["src/**", "pages/**"],
      exclude: [
        "pages/_document.tsx",
        "src/**/*.test.{ts,tsx}",
        "src/test/**",
        // Each is only a dynamic() import of the real map, which has its own test.
        "src/**/*MapWrapper.tsx",
      ],
    },
  },
});
