import { useEffect, useRef } from "react";
import { Button } from "./Button";

/**
 * A modal built on the native `<dialog>`: the browser traps focus, closes it on Escape and makes the page behind
 * inert. `onClose` runs however it closes (Escape, the button or the backdrop).
 */
export function Dialog({
  title,
  onClose,
  children,
  closeLabel = "Close",
}: {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
  closeLabel?: string;
}): React.JSX.Element {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current;
    if (dialog === null || dialog.open) return;
    if (typeof dialog.showModal === "function") dialog.showModal();
    else dialog.setAttribute("open", "");
  }, []);
  return (
    <dialog ref={ref} className="gg-dialog" aria-labelledby="gg-dialog-title" onClose={onClose} onCancel={onClose}>
      <h2 id="gg-dialog-title" className="gg-dialog__title">
        {title}
      </h2>
      {children}
      <Button variant="primary" className="gg-dialog__close" onClick={onClose}>
        {closeLabel}
      </Button>
    </dialog>
  );
}
