import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import LeagueTable from "./LeagueTable";

const entries = [
  { rank: 1, score: 320, initials: "ABC", you: false },
  { rank: 2, score: 300, initials: null, you: false },
];

const rows = () => within(screen.getByRole("list", { name: "League table" })).getAllByRole("listitem");

describe("LeagueTable", () => {
  it("lists rank, initials or ???, and score in order", () => {
    render(<LeagueTable entries={entries} />);

    expect(rows()).toHaveLength(2);
    expect(rows()[0]).toHaveTextContent("1 ABC 320");
    expect(rows()[1]).toHaveTextContent("2 ??? 300");
    expect(rows().some((r) => r.hasAttribute("data-you"))).toBe(false);
  });

  it("shows the player's lower row last, after a gap, and marks it", () => {
    render(<LeagueTable entries={entries} yours={{ rank: 6, score: 120, initials: null, you: true }} />);

    const last = rows()[2];
    expect(last).toHaveTextContent("6 ??? You 120");
    expect(last).toHaveAttribute("data-you");
    expect(last).toHaveAttribute("data-gap");
    expect(rows()[1]).not.toHaveAttribute("data-gap");
  });

  it("marks the player's row within the top, with no gap", () => {
    render(<LeagueTable entries={[entries[0], { ...entries[1], you: true }]} yours={null} />);

    expect(rows()[1]).toHaveAttribute("data-you");
    expect(rows()[1]).not.toHaveAttribute("data-gap");
  });

  it("labels the first row", () => {
    render(<LeagueTable entries={entries} topLabel="To beat" />);

    expect(rows()[0]).toHaveTextContent("To beat");
    expect(rows()[1]).not.toHaveTextContent("To beat");
  });
});
