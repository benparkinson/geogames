import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import GameLayout from "./GameLayout";

describe("GameLayout", () => {
  it("places the top bar, map and panel in their regions", () => {
    render(<GameLayout topBar={<span>TOP</span>} map={<span>MAP</span>} panel={<span>PANEL</span>} />);

    expect(within(screen.getByRole("banner")).getByText("TOP")).toBeInTheDocument();
    expect(within(screen.getByRole("main")).getByText("MAP")).toBeInTheDocument();
    expect(within(screen.getByRole("complementary")).getByText("PANEL")).toBeInTheDocument();
  });

  it("appends an extra class to the map region", () => {
    render(<GameLayout topBar="t" map="m" panel="p" mapClassName="game-map--blank" />);

    expect(screen.getByRole("main")).toHaveClass("game-map", "game-map--blank");
  });

  it("uses only the base class without an extra one", () => {
    render(<GameLayout topBar="t" map="m" panel="p" />);

    expect(screen.getByRole("main").className).toBe("game-map");
  });
});
