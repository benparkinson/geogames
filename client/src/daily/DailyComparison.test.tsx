import { screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { callsTo, mockFetch } from "../test/fetchMock";
import { makeDailyScore } from "../test/fixtures";
import { renderWithClient } from "../test/render";
import DailyComparison from "./DailyComparison";

const URL = "http://localhost:8080/api/daily-scores/2026-11-18?difficulty=NORMAL";

function renderComparison(score = 450, difficulty: "EASY" | "NORMAL" | "HARD" = "NORMAL") {
  return renderWithClient(<DailyComparison date="2026-11-18" difficulty={difficulty} score={score} />);
}

async function expectNothing(fetchMock: ReturnType<typeof mockFetch>) {
  await waitFor(() => expect(callsTo(fetchMock)).toHaveLength(1));
  // Let the query settle.
  await new Promise((r) => setTimeout(r, 20));
  expect(screen.queryByText(/average/)).not.toBeInTheDocument();
  expect(screen.queryByRole("img")).not.toBeInTheDocument();
}

describe("DailyComparison", () => {
  it.each([0, 2])("shows nothing for %i players", async (players) => {
    const fetchMock = mockFetch([{ url: URL, body: makeDailyScore({ players, averageScore: players ? 300 : null }) }]);
    renderComparison();
    await expectNothing(fetchMock);
  });

  it("shows nothing for an error status", async () => {
    const fetchMock = mockFetch([{ url: URL, status: 500 }]);
    renderComparison();
    await expectNothing(fetchMock);
  });

  it("shows nothing for a network error", async () => {
    const fetchMock = mockFetch([{ url: URL, networkError: true }]);
    renderComparison();
    await expectNothing(fetchMock);
  });

  it("shows the average, the top share and the chart for exactly 3 players", async () => {
    mockFetch([
      { url: URL, body: makeDailyScore({ players: 3, averageScore: 250, buckets: [1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0] }) },
    ]);
    renderComparison(450);

    expect(await screen.findByText("Today's average: 250.0 / 500")).toBeInTheDocument();
    // Bucket 9 of 11: one of three players is in it or above.
    expect(screen.getByText("Top 34%")).toBeInTheDocument();
    expect(screen.getByRole("img")).toHaveAttribute("aria-label", expect.stringContaining("Your band: 450–499."));
  });

  it("asks for the player's own league and names it unless it is Normal", async () => {
    const league = (difficulty: string) => `http://localhost:8080/api/daily-scores/2026-11-18?difficulty=${difficulty}`;
    mockFetch([
      { url: league("EASY"), body: makeDailyScore({ difficulty: "EASY", players: 5, averageScore: 400 }) },
      { url: league("HARD"), body: makeDailyScore({ difficulty: "HARD", players: 5, averageScore: 200 }) },
    ]);
    const easy = renderComparison(450, "EASY");
    expect(await screen.findByText("Today's average (Easy players): 400.0 / 500")).toBeInTheDocument();
    easy.unmount();

    renderComparison(450, "HARD");
    expect(await screen.findByText("Today's average (Hard mode players): 200.0 / 500")).toBeInTheDocument();
  });

  it("highlights the player's bar only", async () => {
    mockFetch([{ url: URL, body: makeDailyScore() }]);
    const { container } = renderComparison(300);

    await screen.findByText(/Today's average/);
    const bars = container.querySelectorAll(".gg-daily-chart__bar");
    expect(bars).toHaveLength(11);
    const mine = [...bars].flatMap((b, i) => (b.getAttribute("data-mine") === "true" ? [i] : []));
    expect(mine).toEqual([6]);
    expect(screen.getByRole("img")).toHaveAttribute("aria-label", expect.stringContaining("Your band: 300–349."));
  });

  it("describes the bands as percentages of players", async () => {
    mockFetch([{ url: URL, body: makeDailyScore({ players: 4, buckets: [0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 1] }) }]);
    renderComparison();

    const label = (await screen.findByRole("img")).getAttribute("aria-label");
    expect(label).toContain("150–199: 25%");
    expect(label).toContain("250–299: 50%");
    expect(label).toContain("500 (perfect): 25%");
  });
});
