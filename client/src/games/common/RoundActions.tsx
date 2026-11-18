import { useEffect, useRef, useState } from "react";
import { EyeFill, Lightbulb } from "react-bootstrap-icons";
import { Button } from "../../ui";
import { HintConfig } from "./hints";
import { CORRECT_ANSWER } from "./Model";
import { roundPoints } from "./scoring";

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
  const nextHintCost =
    roundPoints(CORRECT_ANSWER, hintsUsed, hintCount) - roundPoints(CORRECT_ANSWER, hintsUsed + 1, hintCount);
  const hintTitle = hintsLeft > 0 ? `${hints.description} (−${nextHintCost} pts)` : "No hints left";

  return (
    <div className="d-flex flex-wrap gap-2 mb-2">
      <span title={hintTitle}>
        <Button variant="hint" onClick={onHint} disabled={hintsLeft <= 0}>
          <Lightbulb aria-hidden="true" /> Hint ({hintsLeft} left)
        </Button>
      </span>
      <Button variant="danger" onClick={handleRevealClick}>
        <EyeFill aria-hidden="true" /> {confirmReveal ? "Tap again to reveal" : "Reveal answer"}
      </Button>
    </div>
  );
}
