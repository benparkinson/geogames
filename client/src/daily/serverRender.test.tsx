import { QueryClientProvider } from "@tanstack/react-query";
import { renderToString } from "react-dom/server";
import { describe, expect, it } from "vitest";
import DailyCard from "../components/DailyCard";
import SiteHeader from "../components/SiteHeader";
import { newTestClient } from "../test/render";
import DailyFinished from "./DailyFinished";
import DailyIntro from "./DailyIntro";
import DailyPage from "./DailyPage";
import { writeDaily } from "./dailyStorage";
import { localDateString } from "../api/daily";

const withClient = (ui: React.ReactElement) => <QueryClientProvider client={newTestClient()}>{ui}</QueryClientProvider>;

// The page is pre-rendered at build time, where there is no date and no localStorage to read.
describe("the daily page on the server", () => {
  it("renders a spinner whatever is stored, so the build bakes in no date and no game", () => {
    writeDaily({ v: 1, date: localDateString(), number: 9, gameId: "g1", difficulty: "HARD" });

    const html = renderToString(<DailyPage />);

    expect(html).toContain("spinner");
    expect(html).not.toContain("Daily 2026-11-18");
    expect(html).not.toContain("g1");
  });

  it("renders the home page's daily card with no streak, even with one stored", () => {
    localStorage.setItem("gg-daily-streak", JSON.stringify({ v: 1, lastDate: localDateString(), count: 4 }));

    const html = renderToString(withClient(<DailyCard />));

    expect(html).toContain("Daily challenge");
    expect(html).not.toContain("data-streak");
  });
});

describe("the daily card on the server with a finished daily", () => {
  it("renders no countdown", () => {
    writeDaily({
      v: 1,
      date: localDateString(),
      number: 9,
      gameId: "g1",
      difficulty: "NORMAL",
      result: { score: 1, maxScore: 5, rounds: ["good"] },
    });

    expect(renderToString(withClient(<DailyCard />))).not.toContain("data-countdown");
  });
});

describe("the daily pages and the header on the server", () => {
  it("renders the header with no streak, even with one stored", () => {
    localStorage.setItem("gg-daily-streak", JSON.stringify({ v: 1, lastDate: localDateString(), count: 4 }));

    expect(renderToString(<SiteHeader title="Geoggernaut" />)).not.toContain("data-streak");
  });

  it("renders the intro with no date", () => {
    const html = renderToString(withClient(<DailyIntro />));

    expect(html).toContain("Start");
    expect(html).not.toContain("gg-ticket__date");
  });

  it("renders a finished daily with no countdown and no date of today", () => {
    const entry = {
      v: 1 as const,
      date: "2026-01-02",
      number: 9,
      gameId: "g1",
      difficulty: "NORMAL" as const,
      result: { score: 1, maxScore: 5, rounds: ["good" as const] },
    };

    const html = renderToString(withClient(<DailyFinished entry={entry} onOpenGame={() => {}} />));

    expect(html).toContain("Fri 2 Jan 2026");
    expect(html).not.toContain("data-countdown");
  });
});
