import { defineConfig, devices } from "@playwright/test";

const baseURL = process.env.E2E_BASE_URL ?? "http://localhost:8080";
const port = new URL(baseURL).port || "80";
const inCI = !!process.env.CI;

export default defineConfig({
  testDir: "./tests",
  fullyParallel: true,
  // A test that needs a retry to pass is flaky, and CI should say so.
  retries: 0,
  forbidOnly: inCI,
  reporter: inCI ? [["github"], ["html", { open: "never" }]] : [["list"], ["html", { open: "never" }]],
  timeout: 60_000,
  expect: { timeout: 10_000 },
  use: {
    baseURL,
    trace: inCI ? "retain-on-failure" : "on-first-retry",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  projects: [
    { name: "desktop", use: { ...devices["Desktop Chrome"] } },
    // Pixel 7 is Chromium with a small touch screen, which exercises the mobile layout and input.
    { name: "mobile", use: { ...devices["Pixel 7"] } },
  ],
  // The jar serves the built site and the API on one port. Build it first (see README.md).
  webServer: {
    command: `java -jar ../target/geogames-0.0.1-SNAPSHOT.jar --server.port=${port}`,
    url: `${baseURL}/health`,
    reuseExistingServer: !inCI,
    timeout: 120_000,
    stdout: "ignore",
    stderr: "pipe",
  },
});
