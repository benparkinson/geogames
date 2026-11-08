import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { mockFetch } from "../../test/fetchMock";
import { countryNames, riverNames } from "../../test/fixtures";
import { clientWrapper } from "../../test/render";
import { useGuessList } from "./useGuessList";

describe("useGuessList", () => {
  it("has nothing until the list has loaded", async () => {
    mockFetch([{ url: /\/api\/rivers$/, body: riverNames }]);
    const { result } = renderHook(() => useGuessList("rivers"), { wrapper: clientWrapper() });

    expect(result.current.suggestions).toBeUndefined();
    expect(result.current.isKnownGuess).toBeUndefined();
    await waitFor(() => expect(result.current.isKnownGuess).toBeDefined());
  });

  it("knows names and aliases, ignoring case and accents", async () => {
    mockFetch([{ url: /\/api\/rivers$/, body: [...riverNames, { name: "Rhône", additionalNames: [] }] }]);
    const { result } = renderHook(() => useGuessList("rivers"), { wrapper: clientWrapper() });
    await waitFor(() => expect(result.current.isKnownGuess).toBeDefined());

    const known = result.current.isKnownGuess!;
    expect(known("Danube")).toBe(true);
    expect(known("  donau ")).toBe(true);
    expect(known("rhone")).toBe(true);
    expect(known("Danub")).toBe(false);
  });

  it("suggests display names only, sorted and without duplicates", async () => {
    mockFetch([{ url: /\/api\/rivers$/, body: [...riverNames.slice().reverse(), riverNames[0]] }]);
    const { result } = renderHook(() => useGuessList("rivers"), { wrapper: clientWrapper() });

    await waitFor(() => expect(result.current.suggestions).toEqual(["Danube", "Thames"]));
  });

  it("reads the list for the kind it was given", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    const { result } = renderHook(() => useGuessList("countries"), { wrapper: clientWrapper() });

    await waitFor(() => expect(result.current.isKnownGuess?.("Holland")).toBe(true));
  });

  it("stays undefined if the list fails to load", async () => {
    const fetchMock = mockFetch([{ url: /\/api\/rivers$/, status: 503, body: {} }]);
    const { result } = renderHook(() => useGuessList("rivers"), { wrapper: clientWrapper() });

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    await new Promise((resolve) => setTimeout(resolve, 20));

    expect(result.current.suggestions).toBeUndefined();
    expect(result.current.isKnownGuess).toBeUndefined();
  });
});
