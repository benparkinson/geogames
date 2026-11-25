import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import RoundBrief from "./RoundBrief";

describe("RoundBrief", () => {
  it("shows only the question, as the level 1 heading", () => {
    const { container } = render(<RoundBrief question="Which country?" />);

    expect(screen.getByRole("heading", { level: 1, name: "Which country?" })).toBeInTheDocument();
    expect(screen.getAllByRole("heading")).toHaveLength(1);
    expect(container).toHaveTextContent(/^Which country\?$/);
  });

  it("shows the count found beside the question when there are several answers", () => {
    render(<RoundBrief question="Which countries?" found={1} answerCount={3} />);

    expect(screen.getByText("1/3 found")).toBeInTheDocument();
  });

  it("shows no count for a single answer", () => {
    render(<RoundBrief question="Which country?" found={0} answerCount={1} />);

    expect(screen.queryByText(/found$/)).toBeNull();
  });
});
