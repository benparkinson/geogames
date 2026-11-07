import { describe, expect, it } from "vitest";
import { callsTo, mockFetch } from "../test/fetchMock";
import { fetchJson, postJson } from "./http";

describe("fetchJson", () => {
  it("prefixes the server endpoint and returns the parsed body", async () => {
    const fetchMock = mockFetch([{ url: "http://localhost:8080/api/thing", body: { a: 1 } }]);

    await expect(fetchJson("/api/thing", "load thing")).resolves.toEqual({ a: 1 });
    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/thing");
  });

  it("throws what failed and the status when the response is not ok", async () => {
    mockFetch([{ url: /\/api\/thing$/, status: 500, body: {} }]);

    await expect(fetchJson("/api/thing", "load thing")).rejects.toThrow("Failed to load thing (500)");
  });
});

describe("postJson", () => {
  it("sends the body as JSON", async () => {
    const fetchMock = mockFetch([{ method: "POST", url: /\/api\/thing$/, body: { ok: true } }]);

    await postJson("/api/thing", "save thing", { x: 2 });

    const [call] = callsTo(fetchMock);
    expect(call.method).toBe("POST");
    expect(call.body).toEqual({ x: 2 });
    expect(new Headers(call.init?.headers).get("Content-Type")).toBe("application/json");
  });
});
