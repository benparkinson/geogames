import { readFileSync } from "node:fs";

// public/version.json is written by scripts/write-build-info.mjs before every build. Reading it here
// (rather than generating an id) keeps one id per build, however often Next loads this config.
function readBuildId() {
  try {
    const { buildId } = JSON.parse(readFileSync(new URL("./public/version.json", import.meta.url), "utf8"));
    return typeof buildId === "string" && buildId ? buildId : undefined;
  } catch {
    return undefined;
  }
}

/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "export",
  env: { NEXT_PUBLIC_BUILD_ID: readBuildId() },
};
export default nextConfig;
