import { screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import LandingRoute, { getStaticPaths, getStaticProps } from "../../pages/play/[slug]";
import { LANDING, LANDING_TYPES } from "../games/landing";
import { mockFetch } from "../test/fetchMock";
import { renderWithClient } from "../test/render";
import LandingPage from "./LandingPage";

// The route's test lives here, not under pages/, because Next would treat a test file there as a route.

vi.mock("next/router", () => ({ useRouter: () => ({ push: vi.fn(), query: {} }) }));

const HEALTH = "http://localhost:8080/health";

describe("LandingPage", () => {
  it("names the game, offers to start it and wakes the server", () => {
    const fetchMock = mockFetch([{ url: HEALTH, body: "ok" }]);
    renderWithClient(<LandingPage type="TRIPOINT" />);

    expect(screen.getByRole("heading", { level: 1, name: LANDING.TRIPOINT.heading })).toBeInTheDocument();
    const panel = within(screen.getByRole("complementary", { name: "Start a game" }));
    expect(panel.getByRole("button", { name: "Start game" })).toBeInTheDocument();
    expect(panel.queryByRole("link", { name: "How to play" })).toBeNull();
    expect(screen.getByRole("link", { name: "daily challenge" })).toHaveAttribute("href", "/daily");
    expect(fetchMock).toHaveBeenCalledWith(HEALTH, { mode: "no-cors" });
  });

  it("says the site is free apart from the scoring", () => {
    mockFetch([{ url: HEALTH, body: "ok" }]);
    renderWithClient(<LandingPage type="TRIPOINT" />);

    const free = screen.getByText(/Geoggernaut is free/);
    expect(free).toHaveTextContent("Geoggernaut is free, needs no account and is played in your browser.");
    expect(screen.getByText(/Each round is worth up to 10 points/)).not.toHaveTextContent(/free/);
  });

  it("links to the other games' pages", () => {
    mockFetch([{ url: HEALTH, body: "ok" }]);
    renderWithClient(<LandingPage type="TRIPOINT" />);

    const links = within(screen.getByRole("main")).getAllByRole("link");
    const hrefs = links.map((link) => link.getAttribute("href"));
    expect(hrefs).toContain("/play/city-map");
    expect(hrefs).not.toContain("/play/tripoints");
  });
});

describe("the /play/[slug] route", () => {
  it("is built for every landing slug and no other", async () => {
    const { paths, fallback } = await getStaticPaths({});

    expect(fallback).toBe(false);
    expect(paths).toEqual(LANDING_TYPES.map((type) => ({ params: { slug: LANDING[type].slug } })));
  });

  it("finds the game of a slug", async () => {
    expect(await getStaticProps({ params: { slug: "city-map" } })).toEqual({ props: { type: "COUNTRIES_BY_CITIES" } });
    expect(await getStaticProps({ params: { slug: "nowhere" } })).toEqual({ notFound: true });
  });

  it("renders the game's page", () => {
    mockFetch([{ url: HEALTH, body: "ok" }]);
    renderWithClient(<LandingRoute type="LAKES_BY_SHAPE" />);

    expect(screen.getByRole("heading", { level: 1, name: LANDING.LAKES_BY_SHAPE.heading })).toBeInTheDocument();
  });
});
