import { renderToString } from "react-dom/server";
import { describe, expect, it } from "vitest";
import DailyPage from "./DailyPage";
import { writeDaily } from "./dailyStorage";
import { localDateString } from "../api/daily";

// The page is pre-rendered at build time, where there is no date and no localStorage to read.
describe("the daily page on the server", () => {
  it("renders a spinner whatever is stored, so the build bakes in no date and no game", () => {
    writeDaily({ v: 1, date: localDateString(), number: 9, gameId: "g1", difficulty: "HARD" });

    const html = renderToString(<DailyPage />);

    expect(html).toContain("spinner");
    expect(html).not.toContain("Daily 2026-11-18");
    expect(html).not.toContain("g1");
  });
});
