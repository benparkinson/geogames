import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { callsTo, mockFetch } from "../test/fetchMock";
import { clientWrapper } from "../test/render";
import { fetchRound, roundUrl, useCreateGame } from "./games";

const router = vi.hoisted(() => ({ push: vi.fn(), query: {} }));
vi.mock("next/router", () => ({ useRouter: () => router }));

describe("roundUrl", () => {
  it("builds the round path", () => {
    expect(roundUrl("abc", 2)).toBe("/api/games/abc/rounds/2");
  });
});

describe("fetchRound", () => {
  it("fetches the round from the server and returns its JSON", async () => {
    const fetchMock = mockFetch([
      { url: "http://localhost:8080/api/games/g1/rounds/3", body: { totalRoundCount: 5 } },
    ]);

    await expect(fetchRound("g1", 3)).resolves.toEqual({ totalRoundCount: 5 });
    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/games/g1/rounds/3");
  });

  it("rejects with the round and status when the response is not ok", async () => {
    mockFetch([{ url: /rounds\/3$/, status: 404, body: {} }]);

    await expect(fetchRound("g1", 3)).rejects.toThrow("Failed to load round 3 (404)");
  });
});

describe("useCreateGame", () => {
  it("posts the request and navigates to the new game", async () => {
    router.push.mockClear();
    const fetchMock = mockFetch([
      { method: "POST", url: "http://localhost:8080/api/games", body: { id: 7 } },
    ]);
    const { result } = renderHook(() => useCreateGame(), { wrapper: clientWrapper() });

    act(() => result.current.createGame("TRIPOINT", 5));

    await waitFor(() => expect(router.push).toHaveBeenCalledWith("/games/7"));
    const [call] = callsTo(fetchMock);
    expect(call.method).toBe("POST");
    expect(call.body).toEqual({ gameType: "TRIPOINT", numberOfRounds: 5 });
    expect(new Headers(call.init?.headers).get("Content-Type")).toBe("application/json");
  });

  it("is loading while the request is pending", async () => {
    let finish: (value: unknown) => void = () => {};
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise((resolve) => (finish = resolve))),
    );
    const { result } = renderHook(() => useCreateGame(), { wrapper: clientWrapper() });
    expect(result.current.isLoading).toBe(false);

    act(() => result.current.createGame("RANDOM", 3));
    await waitFor(() => expect(result.current.isLoading).toBe(true));

    await act(async () => finish({ ok: true, status: 200, json: async () => ({ id: 1 }) }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));
  });

  it("reports an error and does not navigate when the server refuses", async () => {
    router.push.mockClear();
    mockFetch([{ method: "POST", url: /\/api\/games$/, status: 500, body: {} }]);
    const { result } = renderHook(() => useCreateGame(), { wrapper: clientWrapper() });

    act(() => result.current.createGame("TRIPOINT", 5));

    await waitFor(() => expect(result.current.error).toBeTruthy());
    expect(String(result.current.error)).toContain("Failed to create game (500)");
    expect(router.push).not.toHaveBeenCalled();
  });
});
