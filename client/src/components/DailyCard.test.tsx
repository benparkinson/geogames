import { act, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { localDateString } from "../api/daily";
import { markDailyPlayed, resetDailyPlayedForTests } from "../daily/dailyPlayed";
import { resetDailyStorageForTests } from "../daily/dailyStorage";
import { callsTo, mockFetch } from "../test/fetchMock";
import { makeDailyChallenge } from "../test/fixtures";
import { renderWithClient } from "../test/render";
import DailyCard from "./DailyCard";

const URL = () => `http://localhost:8080/api/daily-challenges/${localDateString()}`;
const TEXT = "Five different geography puzzles, a new challenge every day.";
const TYPES = ["TRIPOINT", "LAKES_BY_SHAPE", "RIVERS_BY_SHAPE", "BORDERS_BY_SHAPE", "COUNTRIES_BY_CITIES"];

beforeEach(() => {
  resetDailyPlayedForTests();
  resetDailyStorageForTests();
});

afterEach(() => {
  vi.useRealTimers();
});

const previewCalls = (fetchMock: ReturnType<typeof mockFetch>) =>
  callsTo(fetchMock).filter((c) => c.url.includes("/api/daily-challenges/"));

describe("DailyCard preview", () => {
  it("sends no request and shows no list to someone who never played", async () => {
    const fetchMock = mockFetch([]);
    renderWithClient(<DailyCard />);

    expect(await screen.findByText(TEXT)).toBeInTheDocument();
    expect(previewCalls(fetchMock)).toHaveLength(0);
    expect(screen.queryByRole("list")).toBeNull();
  });

  it("lists today's games by name for a returning player", async () => {
    markDailyPlayed();
    mockFetch([{ url: URL(), body: makeDailyChallenge({ gameTypes: TYPES }) }]);
    renderWithClient(<DailyCard />);

    const list = await screen.findByRole("list", { name: "Today's games" });
    expect(within(list).getAllByRole("listitem")).toHaveLength(5);
    expect(within(list).getAllByRole("listitem")[0]).toHaveTextContent(/\S/);
    expect(screen.getByText(TEXT)).toBeInTheDocument();
  });

  it.each([
    ["a 404", { url: URL(), status: 404, body: {} }],
    ["a 500", { url: URL(), status: 500, body: {} }],
    ["a network error", { url: URL(), networkError: true }],
  ])("shows no list after %s", async (_name, route) => {
    markDailyPlayed();
    const fetchMock = mockFetch([route]);
    renderWithClient(<DailyCard />);

    await vi.waitFor(() => expect(previewCalls(fetchMock)).toHaveLength(1));
    await act(async () => {});
    expect(screen.queryByRole("list")).toBeNull();
    expect(screen.getByText(TEXT)).toBeInTheDocument();
  });

  it("gives up on a request that never answers after 4 seconds", async () => {
    markDailyPlayed();
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const fetchMock = vi.fn(
      (_url: unknown, init?: RequestInit) =>
        new Promise<Response>((_resolve, reject) => {
          init?.signal?.addEventListener("abort", () => reject(new DOMException("timeout", "TimeoutError")));
        }),
    );
    vi.stubGlobal("fetch", fetchMock);
    renderWithClient(<DailyCard />);

    await act(async () => {
      await vi.advanceTimersByTimeAsync(4500);
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("list")).toBeNull();
    expect(screen.getByText(TEXT)).toBeInTheDocument();
  });

  it("shows no list when a type is unknown", async () => {
    markDailyPlayed();
    const fetchMock = mockFetch([
      { url: URL(), body: makeDailyChallenge({ gameTypes: [...TYPES.slice(0, 4), "FUTURE_GAME"] }) },
    ]);
    renderWithClient(<DailyCard />);

    await vi.waitFor(() => expect(previewCalls(fetchMock)).toHaveLength(1));
    await act(async () => {});
    expect(screen.queryByRole("list")).toBeNull();
    expect(screen.getByText(TEXT)).toBeInTheDocument();
  });
});
