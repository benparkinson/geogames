import { ANSWER_POINTS, NO_HINT_POINTS, NO_MISS_POINTS } from "../games/common/scoring";
import type { PointsBreakdown } from "../games/common/scoring";

type Part = "answer" | "noHints" | "noMisses";

const PARTS: { part: Part; size: number }[] = [
  { part: "answer", size: ANSWER_POINTS },
  { part: "noHints", size: NO_HINT_POINTS },
  { part: "noMisses", size: NO_MISS_POINTS },
];

/** The words for a bar, for a screen reader and the part labels. */
function describe(b: PointsBreakdown): string {
  const total = b.answer + b.noHints + b.noMisses;
  const parts = `answer ${b.answer}, no hints ${b.noHints} of ${NO_HINT_POINTS}, no misses ${b.noMisses} of ${NO_MISS_POINTS}`;
  return `${total} of 10: ${parts}${b.found ? `. Found ${b.found.found} of ${b.found.count}` : ""}`;
}

/**
 * A round's score as 10 cells in a fixed layout: Answer ×4, No hints ×3, No misses ×3. Earned cells are filled in
 * their part's colour and the rest stay empty, so the empty cells show what was missed. With `animate` the earned
 * cells fill one after another (the CSS delays each by its `--i`); the cells are always in the DOM.
 */
export function ScoreBar({
  breakdown,
  animate = false,
  showLabels = false,
  className,
}: {
  breakdown: PointsBreakdown;
  animate?: boolean;
  /** Names the parts under the bar. */
  showLabels?: boolean;
  className?: string;
}): React.JSX.Element {
  let earnedSoFar = 0;
  const groups = PARTS.map(({ part, size }) => {
    const earned = breakdown[part];
    const cells = Array.from({ length: size }, (_, k) => {
      const isEarned = k < earned;
      const order = isEarned ? earnedSoFar++ : undefined;
      return (
        <span
          key={k}
          className="gg-score-bar__cell"
          data-part={part}
          data-earned={isEarned}
          style={order === undefined ? undefined : ({ "--i": order } as React.CSSProperties)}
        />
      );
    });
    return (
      <span key={part} className="gg-score-bar__part">
        {cells}
      </span>
    );
  });
  const labels = breakdown.found
    ? [`Found ${breakdown.found.found} of ${breakdown.found.count}`]
    : [
        `Answer ${breakdown.answer}`,
        `No hints ${breakdown.noHints}/${NO_HINT_POINTS}`,
        `No misses ${breakdown.noMisses}/${NO_MISS_POINTS}`,
      ];
  return (
    <div className={["gg-score-bar", className].filter(Boolean).join(" ")}>
      <div
        className="gg-score-bar__cells"
        role="img"
        aria-label={describe(breakdown)}
        data-animate={animate || undefined}
      >
        {groups}
      </div>
      {showLabels && (
        <ul className="gg-score-bar__labels gg-mono gg-text-muted" aria-hidden="true">
          {labels.map((label) => (
            <li key={label}>{label}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
