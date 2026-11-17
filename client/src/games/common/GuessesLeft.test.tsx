import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import GuessesLeft from "./GuessesLeft";

describe("GuessesLeft", () => {
  it("says how many guesses are left and draws a dot for each allowed guess", () => {
    const { container } = render(<GuessesLeft left={3} limit={5} />);

    expect(screen.getByRole("status", { name: "3 guesses left" })).toBeInTheDocument();
    expect(screen.getByText("3 guesses left")).toHaveClass("gg-text-muted");
    expect(container.querySelectorAll("svg")).toHaveLength(5);
  });

  it("uses the singular and the warning style for the last guess", () => {
    render(<GuessesLeft left={1} limit={5} />);

    expect(screen.getByRole("status", { name: "1 guess left" })).toBeInTheDocument();
    expect(screen.getByText("1 guess left")).toHaveClass("gg-text-warning");
  });

  it("says 0 guesses left", () => {
    render(<GuessesLeft left={0} limit={5} />);

    expect(screen.getByRole("status", { name: "0 guesses left" })).toBeInTheDocument();
  });

  it("hides the dots hidden from screen readers", () => {
    const { container } = render(<GuessesLeft left={2} limit={3} />);

    expect(container.querySelector(".guesses-left-dots")).toHaveAttribute("aria-hidden", "true");
  });

  it("shows only the text for a long limit", () => {
    const { container } = render(<GuessesLeft left={11} limit={12} />);

    expect(screen.getByText("11 guesses left")).toBeInTheDocument();
    expect(container.querySelectorAll("svg")).toHaveLength(0);
  });

  it("renders nothing without a limit", () => {
    const { container } = render(<GuessesLeft left={Infinity} limit={Infinity} />);

    expect(container).toBeEmptyDOMElement();
  });
});
