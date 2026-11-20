import { X } from "react-bootstrap-icons";
import { Button } from "./Button";

/** A small message fixed at the top of the screen, with an optional action and a close button. */
export function Toast({
  onDismiss,
  action,
  children,
}: {
  onDismiss: () => void;
  action?: React.ReactNode;
  children: React.ReactNode;
}): React.JSX.Element {
  return (
    <div role="status" className="gg-toast">
      <span>{children}</span>
      {action}
      <Button variant="quiet" size="sm" aria-label="Dismiss" onClick={onDismiss}>
        <X aria-hidden="true" />
      </Button>
    </div>
  );
}
