import { describe, expect, it, vi } from "vitest";
import { wakeServer } from "./health";

describe("wakeServer", () => {
  it("pings /health without waiting for or caring about the result", async () => {
    const fetchMock = vi.fn(() => Promise.reject(new TypeError("Failed to fetch")));
    vi.stubGlobal("fetch", fetchMock);

    expect(() => wakeServer()).not.toThrow();

    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/health", { mode: "no-cors" });
  });
});
