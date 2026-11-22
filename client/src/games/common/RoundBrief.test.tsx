import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import RoundBrief from "./RoundBrief";
import { ContinentIcon } from "../../ui/icons";

describe("RoundBrief", () => {
  it("shows the round name and the question as a level 1 heading", () => {
    render(<RoundBrief title="Land Borders" Icon={ContinentIcon} question="Which country?" />);

    expect(screen.getByText("Land Borders")).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1, name: "Which country?" })).toBeInTheDocument();
  });
});
