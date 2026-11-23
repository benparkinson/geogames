import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import Ticket, { ticketDate } from "./Ticket";

describe("ticketDate", () => {
  it.each([
    [new Date(2026, 10, 22), "Sun 22 Nov 2026"],
    [new Date(2027, 0, 1), "Fri 1 Jan 2027"],
    [new Date(2026, 11, 31, 23, 59), "Thu 31 Dec 2026"],
  ])("formats %s as %s", (date, expected) => {
    expect(ticketDate(date)).toBe(expected);
  });
});

describe("Ticket", () => {
  it("shows the date in the strip", () => {
    render(
      <Ticket date="Sun 22 Nov 2026" footer="Start">
        Body
      </Ticket>,
    );

    expect(screen.getByText("Sun 22 Nov 2026")).toBeInTheDocument();
  });

  it("leaves the date out while it is unknown (pre-rendering)", () => {
    const { container } = render(
      <Ticket date={null} footer="Start">
        Body
      </Ticket>,
    );

    expect(container).toHaveTextContent(/^Daily challengeBodyStart$/);
  });

  it("makes the strip the page's heading when asked", () => {
    render(
      <Ticket date={null} stripTag="h1" footer="Start">
        Body
      </Ticket>,
    );

    expect(screen.getByRole("heading", { level: 1, name: "Daily challenge" })).toBeInTheDocument();
  });
});
