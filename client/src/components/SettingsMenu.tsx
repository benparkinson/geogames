import { useEffect, useId, useRef, useState } from "react";
import ThemeSelector from "./ThemeSelector";

/** The footer's "Settings" link and the small panel it opens above itself. Closed on the server. */
export default function SettingsMenu(): React.JSX.Element {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLSpanElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const panelId = useId();

  useEffect(() => {
    if (!open) return;
    function onKeyDown(event: KeyboardEvent): void {
      if (event.key === "Escape") {
        setOpen(false);
        buttonRef.current?.focus();
      }
    }
    function onPointerDown(event: PointerEvent): void {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) setOpen(false);
    }
    document.addEventListener("keydown", onKeyDown);
    document.addEventListener("pointerdown", onPointerDown);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.removeEventListener("pointerdown", onPointerDown);
    };
  }, [open]);

  return (
    <span className="gg-settings" ref={rootRef}>
      <button
        type="button"
        ref={buttonRef}
        className="gg-settings__button"
        aria-expanded={open}
        aria-controls={panelId}
        onClick={() => setOpen((wasOpen) => !wasOpen)}
      >
        Settings
      </button>
      {open && (
        <div id={panelId} className="gg-settings__panel">
          <ThemeSelector />
        </div>
      )}
    </span>
  );
}
