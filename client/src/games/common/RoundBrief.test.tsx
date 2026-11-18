import { render, screen } from "@testing-library/react";
import { Globe } from "react-bootstrap-icons";
import { describe, expect, it } from "vitest";
import RoundBrief from "./RoundBrief";

describe("RoundBrief", () => {
  it("shows the round name, the question as a level 1 heading and the explanation", () => {
    render(<RoundBrief title="Land Borders" Icon={Globe} question="Which country?" mapKey="The lines are borders." />);

    expect(screen.getByText("Land Borders")).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1, name: "Which country?" })).toBeInTheDocument();
    expect(screen.getByText("The lines are borders.")).toBeInTheDocument();
  });

  it("leaves out the explanation when there is no map key", () => {
    render(<RoundBrief title="Land Borders" Icon={Globe} question="Which country?" />);

    expect(screen.getByRole("heading", { level: 1 })).toBeInTheDocument();
    expect(document.querySelector(".round-brief-key")).toBeNull();
  });
});
