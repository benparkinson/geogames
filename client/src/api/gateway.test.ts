import { describe, expect, it, vi } from "vitest";
import { client, serverEndpoint } from "./gateway";

describe("serverEndpoint", () => {
  it("points at the deployed service in production", () => {
    vi.stubEnv("NODE_ENV", "production");
    expect(serverEndpoint()).toBe("https://geogames-web-service.onrender.com");
  });

  it.each(["development", "test"])("points at localhost in %s", (env) => {
    vi.stubEnv("NODE_ENV", env);
    expect(serverEndpoint()).toBe("http://localhost:8080");
  });
});

describe("client", () => {
  it("does not refetch on window focus", () => {
    expect(client.getDefaultOptions().queries?.refetchOnWindowFocus).toBe(false);
  });
});
