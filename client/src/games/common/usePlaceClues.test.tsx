import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { mockFetch } from "../../test/fetchMock";
import { countryNames, lakeNames, riverNames } from "../../test/fixtures";
import { clientWrapper, newTestClient } from "../../test/render";
import { useLocatedPlaceNames } from "../../api/placeNames";
import { usePlaceClues } from "./usePlaceClues";

async function loaded() {
  mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
  const { result } = renderHook(() => usePlaceClues("countries"), { wrapper: clientWrapper() });
  await waitFor(() => expect(result.current).toBeDefined());
  return result.current!;
}

describe("usePlaceClues", () => {
  it("is undefined until the country list has loaded", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    const { result } = renderHook(() => usePlaceClues("countries"), { wrapper: clientWrapper() });

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

describe("usePlaceClues for rivers and lakes", () => {
  it("measures between two rivers", async () => {
    mockFetch([{ url: /\/api\/rivers$/, body: riverNames }]);
    const { result } = renderHook(() => usePlaceClues("rivers"), { wrapper: clientWrapper() });
    await waitFor(() => expect(result.current).toBeDefined());

    const clue = result.current!("Donau", ["Thames"])!;

    expect(clue.km).toBeGreaterThan(1600);
    expect(clue.km).toBeLessThan(1800);
    // The Thames is to the north-west of the Danube.
    expect(clue.bearing).toBeGreaterThan(270);
    expect(clue.bearing).toBeLessThan(360);
  });

  it("measures between two lakes", async () => {
    mockFetch([{ url: /\/api\/lakes$/, body: lakeNames }]);
    const { result } = renderHook(() => usePlaceClues("lakes"), { wrapper: clientWrapper() });
    await waitFor(() => expect(result.current).toBeDefined());

    const clue = result.current!("Baikal", ["Lake Victoria"])!;

    expect(clue.km).toBeGreaterThan(8000);
    // Lake Victoria is to the south-west of Lake Baikal.
    expect(clue.bearing).toBeGreaterThan(180);
    expect(clue.bearing).toBeLessThan(270);
  });

  it("gives null for a place without a position", async () => {
    mockFetch([
      {
        url: /\/api\/rivers$/,
        body: [{ name: "Nowhere", additionalNames: [] }, ...riverNames],
      },
    ]);
    const { result } = renderHook(() => usePlaceClues("rivers"), { wrapper: clientWrapper() });
    await waitFor(() => expect(result.current).toBeDefined());

    expect(result.current!("Nowhere", ["Thames"])).toBeNull();
    expect(result.current!("Thames", ["Nowhere"])).toBeNull();
  });
});

describe("usePlaceClues when it is not enabled", () => {
  it("is undefined and fetches nothing", async () => {
    const fetchMock = mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    const { result } = renderHook(() => usePlaceClues("countries", false), { wrapper: clientWrapper() });

    await Promise.resolve();

    expect(result.current).toBeUndefined();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("is undefined even when the country list is already cached", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    const wrapper = clientWrapper(newTestClient());
    const names = renderHook(() => useLocatedPlaceNames("countries"), { wrapper });
    await waitFor(() => expect(names.result.current.data).toBeDefined());

    const enabled = renderHook(() => usePlaceClues("countries", true), { wrapper });
    const disabled = renderHook(() => usePlaceClues("countries", false), { wrapper });

    expect(enabled.result.current).toBeDefined();
    expect(disabled.result.current).toBeUndefined();
  });
});
