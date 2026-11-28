import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { Button } from "../../ui";
import { HintConfig } from "./hints";
import { CORRECT_ANSWER } from "./Model";
import { roundPoints } from "./scoring";
import { RevealIcon, HintIcon } from "../../ui/icons";

/**
 * "Hint −1 pt · 3 left". If it has to wrap it breaks after the dot, and the dot is then hidden (kept in the layout, so
 * hiding it can't let the label fit again and bring it back).
 */
function HintLabel({ cost, left }: { cost: number; left: number }): React.JSX.Element {
  const label = useRef<HTMLSpanElement>(null);
  const [wrapped, setWrapped] = useState(false);

  useLayoutEffect(() => {
    const element = label.current;
    if (!element || typeof ResizeObserver === "undefined") {
      return;
    }
    const measure = () => {
      const [first, second] = Array.from(element.children) as HTMLElement[];
      setWrapped(second.offsetTop > first.offsetTop);
    };
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(element);
    return () => observer.disconnect();
  }, [cost, left]);

  return (
    <span ref={label} className="round-actions__hint-label" data-wrapped={wrapped}>
      <span className="round-actions__nowrap">
        {`Hint −${cost} pt `}
        <span className="round-actions__dot">·</span>
      </span>{" "}
      <span className="round-actions__nowrap">{`${left} left`}</span>
    </span>
  );
}

/** The hint and reveal buttons. Revealing asks for a second tap, which lapses after a few seconds. */
export default function RoundActions({
  hints,
  hintCount,
  hintsUsed,
  onHint,
  onGiveUp,
}: {
  hints: HintConfig;
  hintCount: number;
  hintsUsed: number;
  onHint: () => void;
  onGiveUp: () => void;
}): React.JSX.Element {
  const [confirmReveal, setConfirmReveal] = useState(false);
  const revealTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (revealTimer.current) {
        clearTimeout(revealTimer.current);
      }
    };
  }, []);

  function handleRevealClick() {
    if (!confirmReveal) {
      setConfirmReveal(true);
      revealTimer.current = setTimeout(() => setConfirmReveal(false), 3000);
      return;
    }
    if (revealTimer.current) {
      clearTimeout(revealTimer.current);
    }
    setConfirmReveal(false);
    onGiveUp();
  }

  const hintsLeft = hintCount - hintsUsed;
  // The drop the next hint causes, as a difference so it rounds the same way as the score.
  const nextHintCost = roundPoints(CORRECT_ANSWER, hintsUsed) - roundPoints(CORRECT_ANSWER, hintsUsed + 1);
  const hintTitle =
    hintsLeft > 0
      ? `${hints.description} (−${nextHintCost} ${nextHintCost === 1 ? "point" : "points"})`
      : "No hints left";

  return (
    <div className="round-actions">
      <span title={hintTitle}>
        <Button variant="hint" onClick={onHint} disabled={hintsLeft <= 0}>
          <HintIcon aria-hidden="true" />{" "}
          {hintsLeft > 0 ? <HintLabel cost={nextHintCost} left={hintsLeft} /> : "Hint · 0 left"}
        </Button>
      </span>
      <Button variant="danger" onClick={handleRevealClick}>
        <RevealIcon aria-hidden="true" />{" "}
        {/* Both labels share one grid cell and only one shows, so the button is always as wide as the longer one.
            Widening on the first tap could wrap it onto a new line, out of the panel's scrolled view on a phone. */}
        <span className="round-actions__reveal-label">
          <span aria-hidden={confirmReveal ? true : undefined}>Give up</span>
          <span aria-hidden={confirmReveal ? undefined : true}>Tap again to give up</span>
        </span>
      </Button>
    </div>
  );
}
