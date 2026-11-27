import { useEffect, useState } from "react";
import type { Difficulty } from "../api/types";
import { Dialog, ScoreBar, ScoreBarHeader } from "../ui";
import DailyComparison from "./DailyComparison";
import type { DailyResult, RoundParts } from "./dailyStorage";

/** How long the total takes to count up, and how far apart the bars start, in ms. */
const COUNT_MS = 2800;
const BAR_STAGGER_MS = 550;

function prefersReducedMotion(): boolean {
  return typeof window.matchMedia === "function" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

/**
 * The popup shown once, when the player first sees a finished daily: the five round bars fill one after another and
 * the total counts up. A tap on it jumps to the end. Under reduced motion it shows the final state at once.
 */
export default function DailyScoreReveal({
  date,
  difficulty,
  result,
  parts,
  onClose,
}: {
  date: string;
  difficulty: Difficulty;
  result: DailyResult;
  parts: RoundParts[];
  onClose: () => void;
}): React.JSX.Element {
  const [skipped, setSkipped] = useState(prefersReducedMotion);
  const [counted, setCounted] = useState(0);
  const shown = skipped ? result.score : counted;

  useEffect(() => {
    if (skipped) return;
    const start = Date.now();
    const timer = setInterval(() => {
      const t = Math.min(1, (Date.now() - start) / COUNT_MS);
      setCounted(Math.round(t * result.score));
      if (t >= 1) clearInterval(timer);
    }, 50);
    return () => clearInterval(timer);
  }, [skipped, result.score]);

  return (
    <Dialog title="Your daily score" onClose={onClose} closeLabel="See results">
      {/* A tap anywhere on the scores jumps to the end. */}
      <div className="gg-reveal" data-testid="reveal" onClick={() => setSkipped(true)}>
        <div className="gg-reveal__header">
          <span className="gg-mono gg-reveal__label" />
          <ScoreBarHeader />
        </div>
        <ul className="gg-reveal__bars">
          {result.rounds.map((_, i) => (
            <li
              key={i}
              className="gg-reveal__bar"
              style={{ "--start": `${i * BAR_STAGGER_MS}ms` } as React.CSSProperties}
            >
              <span className="gg-mono gg-reveal__label">{i + 1}</span>
              <ScoreBar breakdown={parts[i]} animate={!skipped} />
            </li>
          ))}
        </ul>
        <p className="gg-reveal__total gg-mono" aria-live="polite">
          {shown} / {result.maxScore}
        </p>
      </div>
      <DailyComparison date={date} difficulty={difficulty} score={result.score} />
    </Dialog>
  );
}
