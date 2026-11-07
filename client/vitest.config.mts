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
      include: ["src/**"],
      exclude: [
        "src/**/*.test.{ts,tsx}",
        "src/test/**",
        "src/map/TripointMap.tsx",
        "src/map/RiverShapesMap.tsx",
        "src/map/TripointMapWrapper.tsx",
        "src/map/RiverShapesMapWrapper.tsx",
      ],
    },
  },
});
