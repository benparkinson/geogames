import { describe, expect, it } from "vitest";
import { callsTo, mockFetch } from "../test/fetchMock";
import { creationErrorMessage, fetchJson, hasStatus, HttpError, postJson } from "./http";

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

describe("HttpError", () => {
  it("is what fetchJson throws, with the status", async () => {
    mockFetch([{ url: /\/api\/thing$/, status: 404, body: {} }]);

    const error = await fetchJson("/api/thing", "load thing").catch((e: unknown) => e);

    expect(error).toBeInstanceOf(HttpError);
    expect((error as HttpError).status).toBe(404);
    expect((error as HttpError).message).toBe("Failed to load thing (404)");
  });
});

describe("hasStatus", () => {
  it("is true only for an HttpError with that status", () => {
    expect(hasStatus(new HttpError("Failed", 404), 404)).toBe(true);
    expect(hasStatus(new HttpError("Failed", 500), 404)).toBe(false);
    expect(hasStatus(new Error("Failed to load thing (404)"), 404)).toBe(false);
    expect(hasStatus(undefined, 404)).toBe(false);
  });
});

describe("creationErrorMessage", () => {
  it("explains a rate limit", () => {
    expect(creationErrorMessage(new HttpError("x", 429))).toBe(
      "Too many new games from your network. Wait a few minutes and try again.",
    );
  });

  it("explains a busy server", () => {
    expect(creationErrorMessage(new HttpError("x", 503))).toBe("The game server is busy. Try again later.");
  });

  it("uses the fallback for anything else", () => {
    expect(creationErrorMessage(new HttpError("x", 500))).toBe(
      "Couldn't start a game. The server may be down. Try again.",
    );
    expect(creationErrorMessage(new Error("x"), "Custom")).toBe("Custom");
  });
});
