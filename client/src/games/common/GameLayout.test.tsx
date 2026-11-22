import { act, render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
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

    expect(screen.getByRole("main")).toHaveAttribute("data-blank-map");
  });

  it("uses only the base class without an extra one", () => {
    render(<GameLayout topBar="t" map="m" panel="p" />);

    expect(screen.getByRole("main").className).toBe("game-map");
  });
});

describe("GameLayout and the on-screen keyboard", () => {
  let resize: () => void = () => {};
  const viewport = {
    height: 300,
    addEventListener: (_: string, listener: () => void) => {
      resize = listener;
    },
    removeEventListener: vi.fn(),
  };

  beforeEach(() => {
    vi.stubGlobal("visualViewport", viewport);
    window.scrollTo = vi.fn() as unknown as typeof window.scrollTo;
    Object.defineProperty(window, "innerHeight", { value: 800, configurable: true });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("fits the screen to the visual viewport and marks it compact", () => {
    viewport.height = 300;
    const { container } = render(<GameLayout topBar="t" map="m" panel="p" />);

    const screenEl = container.firstElementChild as HTMLElement;
    expect(screenEl).toHaveAttribute("data-keyboard", "open");
    expect(screenEl.style.height).toBe("300px");
  });

  it("ignores a resize that changes nothing, and returns to full height when the keyboard closes", () => {
    viewport.height = 300;
    const { container, unmount } = render(<GameLayout topBar="t" map="m" panel="p" />);
    const screenEl = container.firstElementChild as HTMLElement;

    act(() => resize());
    expect(screenEl).toHaveAttribute("data-keyboard", "open");

    viewport.height = 800;
    act(() => resize());
    expect(screenEl).not.toHaveAttribute("data-keyboard");
    expect(screenEl.style.height).toBe("");

    unmount();
    expect(viewport.removeEventListener).toHaveBeenCalled();
  });

  it("does nothing without a visual viewport", () => {
    vi.stubGlobal("visualViewport", null);
    const { container } = render(<GameLayout topBar="t" map="m" panel="p" />);

    expect(container.firstElementChild).not.toHaveAttribute("data-keyboard");
  });
});
