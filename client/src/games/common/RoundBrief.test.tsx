import { render, screen } from "@testing-library/react";
import { Globe } from "react-bootstrap-icons";
import { describe, expect, it } from "vitest";
import RoundBrief from "./RoundBrief";

describe("RoundBrief", () => {
  it("shows the round name and the question as a level 1 heading", () => {
    render(<RoundBrief title="Land Borders" Icon={Globe} question="Which country?" />);

    expect(screen.getByText("Land Borders")).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1, name: "Which country?" })).toBeInTheDocument();
  });
});
