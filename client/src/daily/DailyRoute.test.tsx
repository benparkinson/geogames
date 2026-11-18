import { screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { mockFetch } from "../test/fetchMock";
import { renderWithClient } from "../test/render";
import DailyRoute from "../../pages/daily";

vi.mock("next/router", () => ({ useRouter: () => ({ push: vi.fn(), query: {} }) }));

// The test lives here, not under pages/, because Next would treat a test file there as a route.
describe("the /daily route", () => {
  it("renders the daily page, which makes no game until Play is clicked", async () => {
    const fetchMock = mockFetch([{ url: "http://localhost:8080/health", body: "ok" }]);
    renderWithClient(<DailyRoute />);

    expect(await screen.findByRole("heading", { name: "Daily challenge" })).toBeInTheDocument();
    expect(fetchMock.mock.calls.filter(([, init]) => init?.method === "POST")).toEqual([]);
  });
});
