// Writes public/version.json: {"buildId": "..."}, the id of this build. The static export serves it
// next to the pages, and next.config.mjs inlines the same id into the bundle, so a running page can
// compare its own id with the deployed one and offer a refresh. The id is the commit sha when there
// is one (rebuilding a commit prompts nobody), otherwise a random one.
// Run by `npm run build` and `npm run dev`; the output is generated, so it is git-ignored.
import { execFileSync } from "node:child_process";
import { randomUUID } from "node:crypto";
import { writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");

function gitHead() {
  try {
    return execFileSync("git", ["rev-parse", "HEAD"], { cwd: root, stdio: ["ignore", "pipe", "ignore"] })
      .toString()
      .trim();
  } catch {
    return "";
  }
}

const buildId = process.env.RENDER_GIT_COMMIT || process.env.GITHUB_SHA || gitHead() || randomUUID();

writeFileSync(join(root, "public", "version.json"), JSON.stringify({ buildId }) + "\n", "utf8");
console.log(`Wrote build id ${buildId} to public/version.json`);
