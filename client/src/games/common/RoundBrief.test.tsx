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
});
