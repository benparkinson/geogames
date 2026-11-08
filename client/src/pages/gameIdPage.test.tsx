import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import GameParentPage from "../../pages/games/[gameId]";

const router = vi.hoisted(() => ({ query: {} as Record<string, string | string[] | undefined> }));
vi.mock("next/router", () => ({ useRouter: () => router }));
vi.mock("../games/common/GameRoundPage", () => ({
  default: ({ gameId }: { gameId: string }) => <div data-testid="round-page">{gameId}</div>,
}));

beforeEach(() => {
  router.query = {};
});

describe("game page", () => {
  it("renders nothing until the router knows the game id", () => {
    const { container } = render(<GameParentPage />);

    expect(container).toBeEmptyDOMElement();
  });

  it("renders nothing for a repeated query parameter", () => {
    router.query = { gameId: ["a", "b"] };

    const { container } = render(<GameParentPage />);

    expect(container).toBeEmptyDOMElement();
  });

  it("renders the round page with the game id once it is a string", () => {
    router.query = { gameId: "g42" };

    render(<GameParentPage />);

    expect(screen.getByTestId("round-page")).toHaveTextContent("g42");
  });

  it("starts a fresh round page when the game id changes", () => {
    router.query = { gameId: "g1" };
    const { rerender } = render(<GameParentPage />);
    router.query = { gameId: "g2" };

    rerender(<GameParentPage />);

    expect(screen.getByTestId("round-page")).toHaveTextContent("g2");
  });
});
