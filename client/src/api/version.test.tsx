import { renderHook, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { mockFetch } from "../test/fetchMock";
import { clientWrapper } from "../test/render";
import { useUpdateAvailable } from "./version";

const VERSION = /^\/version\.json\?t=\d+$/;

afterEach(() => vi.unstubAllEnvs());

function setup(enabled = true, ownId: string | null = "mine") {
  vi.stubEnv("NEXT_PUBLIC_BUILD_ID", ownId ?? "");
  return (routes: Parameters<typeof mockFetch>[0]) => {
    const fetchMock = mockFetch(routes);
    const hook = renderHook(() => useUpdateAvailable(enabled), { wrapper: clientWrapper() });
    return { fetchMock, hook };
  };
}

describe("useUpdateAvailable", () => {
  it("is false when the ids match", async () => {
    const { fetchMock, hook } = setup()([{ url: VERSION, body: { buildId: "mine" } }]);
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    await waitFor(() => expect(hook.result.current.latestBuildId).toBe("mine"));
    expect(hook.result.current.updateAvailable).toBe(false);
  });

  it("is true when the ids differ", async () => {
    const { hook } = setup()([{ url: VERSION, body: { buildId: "newer" } }]);
    await waitFor(() => expect(hook.result.current.updateAvailable).toBe(true));
    expect(hook.result.current.latestBuildId).toBe("newer");
  });

  it("fetches from the page's own origin without caching", async () => {
    const { fetchMock, hook } = setup()([{ url: VERSION, body: { buildId: "mine" } }]);
    await waitFor(() => expect(hook.result.current.latestBuildId).toBe("mine"));
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toMatch(VERSION);
    expect(init?.cache).toBe("no-store");
  });

  it.each([
    ["a non-OK status", { url: VERSION, status: 404, body: {} }],
    ["a network error", { url: VERSION, networkError: true }],
    ["a malformed body", { url: VERSION, body: { other: 1 } }],
    ["a body that isn't an object", { url: VERSION, body: "x" }],
    ["an empty id", { url: VERSION, body: { buildId: "" } }],
  ])("is false for %s", async (_name, route) => {
    const { fetchMock, hook } = setup()([route]);
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    await waitFor(() => expect(hook.result.current.updateAvailable).toBe(false));
    expect(hook.result.current.latestBuildId).toBeNull();
  });

  it("never fetches when disabled", () => {
    const { fetchMock, hook } = setup(false)([{ url: VERSION, body: { buildId: "newer" } }]);
    expect(fetchMock).not.toHaveBeenCalled();
    expect(hook.result.current.updateAvailable).toBe(false);
  });

  it("never fetches when the bundle has no id", () => {
    const { fetchMock, hook } = setup(true, null)([{ url: VERSION, body: { buildId: "newer" } }]);
    expect(fetchMock).not.toHaveBeenCalled();
    expect(hook.result.current.updateAvailable).toBe(false);
  });
});
