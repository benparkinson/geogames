import { ANSWER_POINTS, NO_HINT_POINTS, NO_MISS_POINTS } from "../games/common/scoring";
import type { PointsBreakdown } from "../games/common/scoring";

type Part = "answer" | "noHints" | "noMisses";

/** The names of a score's parts, the same wherever they are shown. */
export const SCORE_PART_NAMES: Record<Part, string> = { answer: "Answer", noHints: "Hints", noMisses: "Lives" };

const PARTS: { part: Part; size: number }[] = [
  { part: "answer", size: ANSWER_POINTS },
  { part: "noHints", size: NO_HINT_POINTS },
  { part: "noMisses", size: NO_MISS_POINTS },
];

/** The words for a bar, for a screen reader. */
function describe(b: PointsBreakdown): string {
  const total = b.answer + b.noHints + b.noMisses;
  const sizes = { answer: ANSWER_POINTS, noHints: NO_HINT_POINTS, noMisses: NO_MISS_POINTS };
  const parts = PARTS.map(({ part }) => `${SCORE_PART_NAMES[part].toLowerCase()} ${b[part]} of ${sizes[part]}`).join(
    ", ",
  );
  return `${total} of 10: ${parts}${b.found ? `. Found ${b.found.found} of ${b.found.count}` : ""}`;
}

/**
 * A round's score as 10 cells in a fixed layout: Answer ×4, Hints ×3, Lives ×3. Earned cells are filled in
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
    : // Points, like the banner's total. No "/3": the cells show what each part was worth, and a phone fits it on one line.
      PARTS.map(({ part }) => `${SCORE_PART_NAMES[part]} +${breakdown[part]}`);
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

/** A part's name after a swatch of its colour. */
function PartName({ part }: { part: Part }): React.JSX.Element {
  return (
    <>
      <span className="gg-score-key__swatch" data-part={part} aria-hidden="true" />
      {SCORE_PART_NAMES[part]}
    </>
  );
}

/** Column headers for a stack of bars. Hidden from screen readers: each bar's label already names the parts. */
export function ScoreBarHeader(): React.JSX.Element {
  return (
    <div className="gg-score-bar__labels gg-mono gg-text-muted" aria-hidden="true">
      {PARTS.map(({ part }) => (
        <span key={part}>
          <PartName part={part} />
        </span>
      ))}
    </div>
  );
}

/** The colour key for bars that have no labels. */
export function ScoreKey(): React.JSX.Element {
  return (
    <ul className="gg-score-key gg-mono gg-text-muted">
      {PARTS.map(({ part }) => (
        <li key={part}>
          <PartName part={part} />
        </li>
      ))}
    </ul>
  );
}
