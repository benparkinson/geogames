import { useEffect, useRef, useState } from "react";
import { Button } from "../ui";

const COPIED_MS = 4000;

/**
 * Copies the share text. `navigator.clipboard` needs a secure context and a click, so it is called straight from
 * the handler. If it is missing or refuses, the text is shown selected so the player can copy it by hand.
 */
export default function ShareButton({ text }: { text: string }): React.JSX.Element {
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
      <div className="d-flex flex-wrap align-items-center gap-2">
        <Button variant="primary" onClick={copy}><span aria-hidden="true">📋</span> Copy result</Button>
        {canShare && <Button variant="secondary" onClick={share}><span aria-hidden="true">↗</span> Share</Button>}
        <span aria-live="polite" className="small gg-text-muted">{copied ? "Copied!" : ""}</span>
      </div>
      {manual && (
        <div className="mt-2">
          <label htmlFor="daily-share-text" className="small gg-text-muted">Copy this text</label>
          <textarea
            id="daily-share-text"
            ref={textarea}
            readOnly
            rows={5}
            className="gg-input w-100"
            value={text}
            onFocus={e => e.currentTarget.select()}
          />
        </div>
      )}
    </div>
  );
}
