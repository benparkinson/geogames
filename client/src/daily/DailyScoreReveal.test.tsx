import { act, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { mockFetch } from "../test/fetchMock";
import { renderWithClient } from "../test/render";
import DailyScoreReveal from "./DailyScoreReveal";
import type { DailyResult } from "./dailyStorage";

const result: DailyResult = {
  score: 41,
  maxScore: 50,
  rounds: ["full", "good", "good", "partial", "full"],
  parts: [
    { answer: 4, noHints: 3, noMisses: 3 },
    { answer: 4, noHints: 3, noMisses: 1 },
    { answer: 4, noHints: 2, noMisses: 2 },
    { answer: 4, noHints: 2, noMisses: 0 },
    { answer: 4, noHints: 3, noMisses: 3 },
  ],
};

function show(onClose = vi.fn()) {
  mockFetch([]);
  renderWithClient(
    <DailyScoreReveal date="2026-11-18" difficulty="NORMAL" result={result} parts={result.parts!} onClose={onClose} />,
  );
  return onClose;
}

function stubMotion(reduce: boolean) {
  vi.stubGlobal("matchMedia", (query: string) => ({ matches: reduce, media: query }));
}

beforeEach(() => vi.useFakeTimers({ shouldAdvanceTime: true }));
afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe("DailyScoreReveal", () => {
  it("fills the bars and counts the total up to the score", () => {
    stubMotion(false);
    show();
    expect(screen.getByText("0 / 50")).toBeInTheDocument();
    expect(screen.getAllByRole("img", { name: /of 10/ })).toHaveLength(5);

    act(() => {
      vi.advanceTimersByTime(3500);
    });

    expect(screen.getByText("41 / 50")).toBeInTheDocument();
  });

  it("heads the bar columns with the part names", () => {
    stubMotion(true);
    show();
    for (const name of ["Answer", "Hints", "Lives"]) expect(screen.getByText(name)).toBeInTheDocument();
  });

  it("jumps to the end on a tap", async () => {
    stubMotion(false);
    show();
    expect(screen.getByText("0 / 50")).toBeInTheDocument();

    await userEvent.setup({ advanceTimers: vi.advanceTimersByTime }).click(screen.getByTestId("reveal"));

    expect(screen.getByText("41 / 50")).toBeInTheDocument();
    expect(document.querySelector("[data-animate]")).toBeNull();
  });

  it("shows the final state at once under reduced motion", () => {
    stubMotion(true);
    show();
    expect(screen.getByText("41 / 50")).toBeInTheDocument();
    expect(document.querySelector("[data-animate]")).toBeNull();
  });

  it("closes from the See results button", async () => {
    stubMotion(true);
    const onClose = show();
    await userEvent
      .setup({ advanceTimers: vi.advanceTimersByTime })
      .click(screen.getByRole("button", { name: "See results" }));
    expect(onClose).toHaveBeenCalled();
  });
});
