import { afterEach, describe, expect, it, vi } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { localDateString, useCreateDailyGame } from "./daily";
import { makeDailyGameResponse } from "../test/fixtures";
import { clientWrapper } from "../test/render";

afterEach(() => {
  vi.unstubAllGlobals();
  vi.useRealTimers();
});

describe("localDateString", () => {
  it("is the local date just before and just after local midnight", () => {
    expect(localDateString(new Date(2026, 10, 17, 23, 59, 59))).toBe("2026-11-17");
    expect(localDateString(new Date(2026, 10, 18, 0, 0, 1))).toBe("2026-11-18");
  });

  it("pads the month and the day", () => {
    expect(localDateString(new Date(2027, 0, 5, 12))).toBe("2027-01-05");
  });

  it("is not the UTC date", () => {
    // 23:30 at UTC-5 is already tomorrow in UTC; a date built from local fields ignores that.
    const now = new Date(2026, 10, 17, 23, 30);
    expect(localDateString(now)).toBe("2026-11-17");
  });
});

function stubFetch(...responses: Array<{ status: number; body?: unknown }>) {
  const fetchMock = vi.fn();
  for (const r of responses) {
    fetchMock.mockResolvedValueOnce({ ok: r.status < 400, status: r.status, json: async () => r.body ?? {} });
  }
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("useCreateDailyGame", () => {
  it("posts the date and returns the response without navigating", async () => {
    const response = makeDailyGameResponse();
    const fetchMock = stubFetch({ status: 200, body: response });
    const { result } = renderHook(() => useCreateDailyGame(), { wrapper: clientWrapper() });

    result.current.mutate("2026-11-18");

    await waitFor(() => expect(result.current.data).toEqual(response));
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({ date: "2026-11-18" });
  });

  it("asks again with no date after a 400", async () => {
    const response = makeDailyGameResponse({ date: "2026-11-19" });
    const fetchMock = stubFetch({ status: 400 }, { status: 200, body: response });
    const { result } = renderHook(() => useCreateDailyGame(), { wrapper: clientWrapper() });

    result.current.mutate("2031-01-01");

    await waitFor(() => expect(result.current.data).toEqual(response));
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({});
  });

  it("does not retry other errors", async () => {
    const fetchMock = stubFetch({ status: 500 });
    const { result } = renderHook(() => useCreateDailyGame(), { wrapper: clientWrapper() });

    result.current.mutate("2026-11-18");

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
