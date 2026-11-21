import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { addedAttributions } from "../test/leafletMock";
import { GameMap } from "./GameMap";

vi.mock("react-leaflet", async () => await import("../test/leafletMock"));

beforeEach(() => vi.clearAllMocks());

describe("GameMap", () => {
  it("renders its children", () => {
    render(
      <GameMap>
        <p>a layer</p>
      </GameMap>,
    );

    expect(screen.getByText("a layer")).toBeInTheDocument();
  });

  it("adds the standard credit", () => {
    render(<GameMap>{null}</GameMap>);

    expect(addedAttributions()).toEqual(['Map data: Natural Earth | <a href="/privacy">Credits and privacy</a>']);
  });

  it("puts its own credit before the standard one", () => {
    render(<GameMap credit="Tripoints: Wikipedia">{null}</GameMap>);

    expect(addedAttributions()).toEqual([
      'Tripoints: Wikipedia | Map data: Natural Earth | <a href="/privacy">Credits and privacy</a>',
    ]);
  });
});
