import { ReactNode, useEffect, useState } from "react";

/** Below this share of the window's height, the visual viewport is taken to be shrunk by the on-screen keyboard. */
const KEYBOARD_RATIO = 0.8;

/**
 * Follows the visual viewport, which shrinks when a phone's keyboard opens (the layout viewport and `dvh` don't, so
 * the panel would otherwise slide under the keyboard). Gives the height to use and whether the keyboard seems open.
 */
function useVisualViewport(): { height: number | null; keyboardOpen: boolean } {
  const [state, setState] = useState<{ height: number | null; keyboardOpen: boolean }>({
    height: null,
    keyboardOpen: false,
  });

  useEffect(() => {
    const viewport = window.visualViewport;
    if (!viewport) {
      return;
    }
    function update() {
      if (!viewport) {
        return;
      }
      const keyboardOpen = viewport.height < window.innerHeight * KEYBOARD_RATIO;
      setState((previous) =>
        previous.keyboardOpen === keyboardOpen && previous.height === (keyboardOpen ? viewport.height : null)
          ? previous
          : { height: keyboardOpen ? viewport.height : null, keyboardOpen },
      );
      if (keyboardOpen) {
        // The browser scrolls the page to bring the focused input into view; the screen already fits.
        window.scrollTo(0, 0);
      }
    }
    update();
    viewport.addEventListener("resize", update);
    return () => viewport.removeEventListener("resize", update);
  }, []);

  return state;
}

export default function GameLayout({
  topBar,
  map,
  panel,
  mapClassName,
}: {
  topBar: ReactNode;
  map: ReactNode;
  panel: ReactNode;
  mapClassName?: string;
}) {
  const { height, keyboardOpen } = useVisualViewport();
  return (
    <div
      className={"game-screen" + (keyboardOpen ? " game-screen--keyboard" : "")}
      style={height === null ? undefined : { height }}
    >
      <header className="game-top">{topBar}</header>
      <main className={"game-map" + (mapClassName ? " " + mapClassName : "")}>{map}</main>
      <aside className="game-panel">{panel}</aside>
    </div>
  );
}
