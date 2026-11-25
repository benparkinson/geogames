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
    // Some Android browsers shrink the layout viewport with the keyboard, so compare with the
    // tallest visual viewport seen at this width instead of with innerHeight alone.
    let baseline = window.innerHeight;
    let baselineWidth = window.innerWidth;
    function update(visible: VisualViewport) {
      if (window.innerWidth !== baselineWidth) {
        baselineWidth = window.innerWidth;
        baseline = window.innerHeight;
      }
      baseline = Math.max(baseline, visible.height, window.innerHeight);
      const keyboardOpen = visible.height < baseline * KEYBOARD_RATIO;
      const height = keyboardOpen ? visible.height : null;
      setState((previous) =>
        previous.height === height && previous.keyboardOpen === keyboardOpen ? previous : { height, keyboardOpen },
      );
      if (keyboardOpen) {
        // The browser scrolls the page to bring the focused input into view; the screen already fits.
        window.scrollTo(0, 0);
      }
    }
    const onResize = () => update(viewport);
    onResize();
    viewport.addEventListener("resize", onResize);
    return () => viewport.removeEventListener("resize", onResize);
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
      data-keyboard={keyboardOpen ? "open" : undefined}
      style={height === null ? undefined : { height }}
    >
      <header className="game-top">{topBar}</header>
      <main
        className={"game-map" + (mapClassName ? " " + mapClassName : "")}
        data-blank-map={mapClassName ? "" : undefined}
      >
        {map}
      </main>
      <aside className="game-panel">{panel}</aside>
    </div>
  );
}
