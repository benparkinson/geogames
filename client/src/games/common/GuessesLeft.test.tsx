import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import GuessesLeft from "./GuessesLeft";

describe("GuessesLeft", () => {
  it("shows the lives left out of the total and draws a heart for each life", () => {
    const { container } = render(<GuessesLeft left={3} limit={5} />);

    expect(screen.getByRole("status", { name: "3 lives left" })).toBeInTheDocument();
    expect(screen.getByText("3 of 5 lives left")).toBeInTheDocument();
    expect(container.querySelectorAll("svg")).toHaveLength(5);
  });

  it("shows the raised limit of an Easy round", () => {
    render(<GuessesLeft left={8} limit={8} />);

    expect(screen.getByRole("status", { name: "8 lives left" })).toBeInTheDocument();
    expect(screen.getByText("8 of 8 lives left")).toBeInTheDocument();
  });

  it("explains that a wrong guess costs a life", () => {
    render(<GuessesLeft left={3} limit={5} />);

    expect(screen.getByRole("status")).toHaveAttribute("title", expect.stringContaining("wrong guess costs a life"));
  });

  it("uses the singular for the last life", () => {
    render(<GuessesLeft left={1} limit={5} />);

    expect(screen.getByRole("status", { name: "1 life left" })).toBeInTheDocument();
    expect(screen.getByText("Last life!")).toBeInTheDocument();
  });

  it("says 0 lives left", () => {
    render(<GuessesLeft left={0} limit={5} />);

    expect(screen.getByRole("status", { name: "0 lives left" })).toBeInTheDocument();
  });

  it("hides the hearts from screen readers", () => {
    const { container } = render(<GuessesLeft left={2} limit={3} />);

    expect(container.querySelector("svg")?.closest("[aria-hidden]")).toHaveAttribute("aria-hidden", "true");
  });

  it("shows only the text for a long limit", () => {
    const { container } = render(<GuessesLeft left={11} limit={12} />);

    expect(screen.getByText("11 of 12 lives left")).toBeInTheDocument();
    expect(container.querySelectorAll("svg")).toHaveLength(0);
  });
});
