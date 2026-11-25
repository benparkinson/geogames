import { useEffect, useId, useRef, useState } from "react";
import { Button } from "../ui";

const COPIED_MS = 4000;

/**
 * Copies the share text; the button briefly says Copied. `navigator.clipboard` needs a secure context and a click, so it is called straight from
 * the handler. If it is missing or refuses, the text is shown selected so the player can copy it by hand.
 */
export default function ShareButton({
  text,
  label = "Copy result",
}: {
  text: string;
  /** What the copy button says before it is clicked. */
  label?: string;
}): React.JSX.Element {
  const textareaId = useId();
  const [copied, setCopied] = useState(false);
  const [manual, setManual] = useState(false);
  const textarea = useRef<HTMLTextAreaElement>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const canShare = typeof navigator !== "undefined" && typeof navigator.share === "function";

  useEffect(() => () => clearTimeout(timer.current), []);
  useEffect(() => {
    if (manual) textarea.current?.select();
  }, [manual]);

  function copy(): void {
    if (typeof navigator === "undefined" || !navigator.clipboard) {
      setManual(true);
      return;
    }
    navigator.clipboard.writeText(text).then(
      () => {
        setManual(false);
        setCopied(true);
        clearTimeout(timer.current);
        timer.current = setTimeout(() => setCopied(false), COPIED_MS);
      },
      () => setManual(true),
    );
  }

  function share(): void {
    // Cancelling the share sheet rejects, which is not an error worth showing.
    navigator.share({ text }).catch(() => {});
  }

  return (
    <div>
      <div className="gg-share__row">
        <Button variant="primary" onClick={copy}>
          {/* Both labels share one grid cell, so the button keeps the wider width while the other is hidden. */}
          <span style={{ display: "inline-grid" }}>
            <span style={{ gridArea: "1 / 1", visibility: copied ? "hidden" : "visible" }}>
              <span aria-hidden="true">📋</span> {label}
            </span>
            <span style={{ gridArea: "1 / 1", visibility: copied ? "visible" : "hidden" }}>
              <span aria-hidden="true">✅</span> Copied
            </span>
          </span>
        </Button>
        {canShare && (
          <Button variant="secondary" onClick={share}>
            <span aria-hidden="true">↗</span> Share
          </Button>
        )}
      </div>
      {manual && (
        <div className="gg-share__manual">
          <label htmlFor={textareaId} className="gg-share__label gg-text-muted">
            Copy this text
          </label>
          <textarea
            id={textareaId}
            ref={textarea}
            readOnly
            rows={5}
            className="gg-input gg-share__text"
            value={text}
            onFocus={(e) => e.currentTarget.select()}
          />
        </div>
      )}
    </div>
  );
}
