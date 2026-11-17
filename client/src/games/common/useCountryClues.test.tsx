import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { mockFetch } from "../../test/fetchMock";
import { countryNames } from "../../test/fixtures";
import { clientWrapper } from "../../test/render";
import { useCountryClues } from "./useCountryClues";

async function loaded() {
  mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
  const { result } = renderHook(() => useCountryClues(), { wrapper: clientWrapper() });
  await waitFor(() => expect(result.current).toBeDefined());
  return result.current!;
}

describe("useCountryClues", () => {
  it("is undefined until the country list has loaded", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    const { result } = renderHook(() => useCountryClues(), { wrapper: clientWrapper() });

    expect(result.current).toBeUndefined();
    await waitFor(() => expect(result.current).toBeDefined());
  });

  it("locates a guess by an alias", async () => {
    const clueFor = await loaded();

    const clue = clueFor("deutschland", ["France"])!;

    expect(clue.km).toBeGreaterThan(500);
    expect(clue.km).toBeLessThan(1000);
    // France is to the west of Germany.
    expect(clue.bearing).toBeGreaterThan(225);
    expect(clue.bearing).toBeLessThan(315);
  });

  it("measures to the nearest of several targets", async () => {
    const clueFor = await loaded();

    const nearest = clueFor("France", ["Germany", "Belgium"])!;
    const belgium = clueFor("France", ["Belgium"])!;

    expect(nearest).toEqual(belgium);
  });

  it("gives null when the guess or every target is missing", async () => {
    const clueFor = await loaded();

    expect(clueFor("Atlantis", ["France"])).toBeNull();
    expect(clueFor("France", ["Atlantis"])).toBeNull();
    expect(clueFor("France", [])).toBeNull();
  });
});
