import { afterEach, describe, expect, it, vi } from "vitest";
import { serverEndpoint } from "./gateway";

describe("serverEndpoint", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("points at the deployed service in production", () => {
    vi.stubEnv("NODE_ENV", "production");
    vi.stubEnv("NEXT_PUBLIC_API_URL", undefined);
    expect(serverEndpoint()).toBe("https://geogames-web-service.onrender.com");
  });

  it.each(["development", "test"])("points at localhost in %s", (env) => {
    vi.stubEnv("NODE_ENV", env);
    vi.stubEnv("NEXT_PUBLIC_API_URL", undefined);
    expect(serverEndpoint()).toBe("http://localhost:8080");
  });

  it.each(["production", "development"])("uses NEXT_PUBLIC_API_URL when it is set, in %s", (env) => {
    vi.stubEnv("NODE_ENV", env);
    vi.stubEnv("NEXT_PUBLIC_API_URL", "http://localhost:9090");
    expect(serverEndpoint()).toBe("http://localhost:9090");
  });

  it.each([
    ["production", "https://geogames-web-service.onrender.com"],
    ["development", "http://localhost:8080"],
  ])("ignores an empty NEXT_PUBLIC_API_URL in %s", (env, expected) => {
    vi.stubEnv("NODE_ENV", env);
    vi.stubEnv("NEXT_PUBLIC_API_URL", "");
    expect(serverEndpoint()).toBe(expected);
  });
});
