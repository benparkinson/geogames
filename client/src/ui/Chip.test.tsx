import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Chip } from "./Chip";

describe("Chip", () => {
  it("has the tone class and forwards attributes", () => {
    render(<Chip tone="revealed" title="Revealed" className="slot--filled">France</Chip>);

    const chip = screen.getByText("France");
    expect(chip).toHaveClass("gg-chip", "gg-chip--revealed", "slot--filled");
    expect(chip).not.toHaveClass("gg-chip--sm", "gg-chip--strike");
    expect(chip).toHaveAttribute("title", "Revealed");
  });

  it("can be small and struck through", () => {
    render(<Chip tone="neutral" size="sm" strike>Spain</Chip>);

    expect(screen.getByText("Spain")).toHaveClass("gg-chip--neutral", "gg-chip--sm", "gg-chip--strike");
  });
});
