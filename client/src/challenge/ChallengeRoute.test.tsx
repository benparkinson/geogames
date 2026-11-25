import { screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { mockFetch } from "../test/fetchMock";
import { makeChallengeResponse } from "../test/fixtures";
import { renderWithClient } from "../test/render";
import ChallengeRoute from "../../pages/challenge";

const router = vi.hoisted(() => ({ replace: vi.fn(), query: {} as Record<string, string>, isReady: true }));
vi.mock("next/router", () => ({ useRouter: () => router }));

// The test lives here, not under pages/, because Next would treat a test file there as a route.
describe("the /challenge route", () => {
  it("loads the challenge named in the link and makes no game", async () => {
    router.query = { id: "c1" };
    router.isReady = true;
    const fetchMock = mockFetch([
      { url: "http://localhost:8080/api/challenges/c1", body: makeChallengeResponse({ roundCount: 1 }) },
      { url: "http://localhost:8080/health", body: "ok" },
    ]);
    renderWithClient(<ChallengeRoute />);

    expect(await screen.findByText(/Mixed · 1 round$/)).toBeInTheDocument();
    expect(fetchMock.mock.calls.filter(([, init]) => init?.method === "POST")).toEqual([]);
  });

  it("waits for the router before asking for anything", () => {
    router.query = {};
    router.isReady = false;
    const fetchMock = mockFetch([{ url: "http://localhost:8080/health", body: "ok" }]);
    renderWithClient(<ChallengeRoute />);

    expect(fetchMock.mock.calls.filter(([url]) => String(url).includes("/api/"))).toEqual([]);
  });

  it("treats a link with no id as a challenge that can't be found", async () => {
    router.query = {};
    router.isReady = true;
    mockFetch([
      { url: "http://localhost:8080/api/challenges/", status: 404, body: {} },
      { url: "http://localhost:8080/health", body: "ok" },
    ]);
    renderWithClient(<ChallengeRoute />);

    expect(await screen.findByRole("heading", { name: "This challenge has expired" })).toBeInTheDocument();
  });
});
