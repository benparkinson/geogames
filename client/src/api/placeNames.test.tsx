import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { countryNames, riverNames } from "../test/fixtures";
import { mockFetch } from "../test/fetchMock";
import { clientWrapper, newTestClient } from "../test/render";
import { usePlaceNames } from "./placeNames";

describe("usePlaceNames", () => {
  it("returns the country list from the server", async () => {
    mockFetch([{ url: "http://localhost:8080/api/countries", body: countryNames }]);
    const { result } = renderHook(() => usePlaceNames("countries"), { wrapper: clientWrapper() });

    await waitFor(() => expect(result.current.data).toEqual(countryNames));
  });

  it("returns the river list from the river endpoint", async () => {
    mockFetch([{ url: "http://localhost:8080/api/rivers", body: riverNames }]);
    const { result } = renderHook(() => usePlaceNames("rivers"), { wrapper: clientWrapper() });

    await waitFor(() => expect(result.current.data).toEqual(riverNames));
  });

  it("uses the lake endpoint for lakes", async () => {
    const lakes = [{ name: "Superior", additionalNames: [] as string[] }];
    mockFetch([{ url: "http://localhost:8080/api/lakes", body: lakes }]);
    const { result } = renderHook(() => usePlaceNames("lakes"), { wrapper: clientWrapper() });

    await waitFor(() => expect(result.current.data).toEqual(lakes));
  });

  it("fetches only once for hooks sharing a client", async () => {
    const fetchMock = mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    const wrapper = clientWrapper(newTestClient());

    const first = renderHook(() => usePlaceNames("countries"), { wrapper });
    await waitFor(() => expect(first.result.current.data).toEqual(countryNames));
    const second = renderHook(() => usePlaceNames("countries"), { wrapper });

    expect(second.result.current.data).toEqual(countryNames);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("does not share a cache entry between kinds", async () => {
    const fetchMock = mockFetch([
      { url: /\/api\/countries$/, body: countryNames },
      { url: /\/api\/rivers$/, body: riverNames },
    ]);
    const wrapper = clientWrapper(newTestClient());

    const countries = renderHook(() => usePlaceNames("countries"), { wrapper });
    const rivers = renderHook(() => usePlaceNames("rivers"), { wrapper });

    await waitFor(() => expect(countries.result.current.data).toEqual(countryNames));
    await waitFor(() => expect(rivers.result.current.data).toEqual(riverNames));
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("retries once and then reports the error", async () => {
    const fetchMock = mockFetch([{ url: /\/api\/rivers$/, status: 503, body: {} }]);
    const { result } = renderHook(() => usePlaceNames("rivers"), { wrapper: clientWrapper() });

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(String(result.current.error)).toContain("Failed to load rivers (503)");
  });
});
