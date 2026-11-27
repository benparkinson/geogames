import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const publicFile = (name: string): Buffer => readFileSync(join(process.cwd(), "public", name));

/** Width x height from a PNG's header: the signature, then the IHDR chunk, which comes first. */
function pngSize(name: string): string {
  const png = publicFile(name);
  expect(png.subarray(1, 4).toString("ascii")).toBe("PNG");
  return `${png.readUInt32BE(16)}x${png.readUInt32BE(20)}`;
}

const manifest = JSON.parse(publicFile("manifest.json").toString("utf8")) as {
  id: string;
  start_url: string;
  icons: { src: string; sizes: string; type: string; purpose?: string }[];
};

describe("app icons", () => {
  it("opens the installed app on the home page", () => {
    expect(manifest.id).toBe("/");
    expect(manifest.start_url).toBe("/");
  });

  it("has every PNG the manifest lists, at the size it states", () => {
    const pngs = manifest.icons.filter((icon) => icon.type === "image/png");
    expect(pngs.map((icon) => [icon.src, icon.purpose])).toEqual([
      ["icon-192.png", undefined],
      ["icon-512.png", undefined],
      ["icon-maskable-512.png", "maskable"],
    ]);
    for (const icon of pngs) expect(pngSize(icon.src), icon.src).toBe(icon.sizes);
  });

  it("has a 180px Apple touch icon", () => {
    expect(pngSize("apple-touch-icon.png")).toBe("180x180");
  });
});
