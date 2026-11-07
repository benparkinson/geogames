import { describe, expect, it, vi } from "vitest";
import isProduction from "./environment";

describe("isProduction", () => {
  it("is true in production", () => {
    vi.stubEnv("NODE_ENV", "production");
    expect(isProduction()).toBe(true);
  });

  it.each(["development", "test"])("is false in %s", (env) => {
    vi.stubEnv("NODE_ENV", env);
    expect(isProduction()).toBe(false);
  });
});
