import { useEffect, useId, useRef, useState } from "react";
import DistanceUnitSelector from "./DistanceUnitSelector";
import ThemeSelector from "./ThemeSelector";
import { SettingsIcon } from "../ui/icons";

/** The home page header's cog button and the small panel it opens. Closed on the server. */
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
        aria-label="Settings"
        aria-expanded={open}
        aria-controls={panelId}
        onClick={() => setOpen((wasOpen) => !wasOpen)}
      >
        <SettingsIcon aria-hidden="true" />
      </button>
      {open && (
        <div id={panelId} className="gg-settings__panel">
          <ThemeSelector />
          <DistanceUnitSelector />
        </div>
      )}
    </span>
  );
}
