import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { countryNames } from "../test/fixtures";
import { mockFetch } from "../test/fetchMock";
import { clientWrapper, newTestClient } from "../test/render";
import { useCountryNames } from "./countries";

describe("useCountryNames", () => {
  it("returns the country list from the server", async () => {
    mockFetch([{ url: "http://localhost:8080/api/countries", body: countryNames }]);
    const { result } = renderHook(() => useCountryNames(), { wrapper: clientWrapper() });

    await waitFor(() => expect(result.current.data).toEqual(countryNames));
  });

  it("fetches only once for hooks sharing a client", async () => {
    const fetchMock = mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    const wrapper = clientWrapper(newTestClient());

    const first = renderHook(() => useCountryNames(), { wrapper });
    await waitFor(() => expect(first.result.current.data).toEqual(countryNames));
    const second = renderHook(() => useCountryNames(), { wrapper });

    expect(second.result.current.data).toEqual(countryNames);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("retries once and then reports the error", async () => {
    const fetchMock = mockFetch([{ url: /\/api\/countries$/, status: 503, body: {} }]);
    const { result } = renderHook(() => useCountryNames(), { wrapper: clientWrapper() });

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(String(result.current.error)).toContain("Failed to load countries (503)");
  });
});
