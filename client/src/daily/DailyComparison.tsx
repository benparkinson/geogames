import { useDailyScore } from "../api/daily";
import type { Difficulty } from "../api/types";
import { MIN_PLAYERS, scoreBucket, topPercent } from "./dailyScore";

/** Who the average is of, when it isn't the Normal league. */
const LEAGUE_NAMES: Record<Difficulty, string> = { EASY: " (Easy players)", NORMAL: "", HARD: " (Hard mode players)" };

/** The lower bound of each of the 11 bands; the last is a perfect score. */
function bandLabel(bucket: number, maxScore: number): string {
  return String(Math.round((bucket * maxScore) / 10));
}

function bandRange(bucket: number, maxScore: number): string {
  if (bucket === 10) return `${maxScore} (perfect)`;
  const low = Math.round((bucket * maxScore) / 10);
  const high = Math.round(((bucket + 1) * maxScore) / 10) - 1;
  return `${low}–${high}`;
}

/**
 * How the player did against everyone who finished the day's challenge: the average, a rough "Top X%" and the
 * distribution of scores. It is an extra, so it renders nothing while loading, on an error and below `MIN_PLAYERS`.
 */
export default function DailyComparison({
  date,
  difficulty,
  score,
}: {
  date: string;
  difficulty: Difficulty;
  score: number;
}): React.JSX.Element {
  const { data } = useDailyScore(date, difficulty);
  const shown = data !== undefined && data.averageScore !== null && data.players >= MIN_PLAYERS;
  return <div aria-live="polite">{shown && <Comparison data={data} difficulty={difficulty} score={score} />}</div>;
}

function Comparison({
  data,
  difficulty,
  score,
}: {
  data: NonNullable<ReturnType<typeof useDailyScore>["data"]>;
  difficulty: Difficulty;
  score: number;
}): React.JSX.Element {
  const { buckets, players, maxScore, averageScore } = data;
  const mine = scoreBucket(score, maxScore);
  const tallest = Math.max(...buckets, 1);
  const percentOf = (count: number) => Math.round((100 * count) / players);
  const alt =
    `Scores today: ` +
    buckets.map((count, i) => `${bandRange(i, maxScore)}: ${percentOf(count)}%`).join(", ") +
    `. Your band: ${bandRange(mine, maxScore)}.`;
  return (
    <div className="gg-daily-comparison mb-3">
      <p className="mb-1">
        Today&apos;s average{LEAGUE_NAMES[difficulty]}: {averageScore!.toFixed(1)} / {maxScore}
      </p>
      <p className="mb-2">Top {topPercent(buckets, mine, players)}%</p>
      <div role="img" aria-label={alt}>
        <div className="gg-daily-chart" aria-hidden="true">
          {buckets.map((count, i) => (
            <div key={i} className="gg-daily-chart__column">
              <div
                className="gg-daily-chart__bar"
                data-mine={i === mine ? "true" : undefined}
                style={{ height: `${Math.max(2, (100 * count) / tallest)}%` }}
              />
              <span className="gg-daily-chart__label" data-sparse={i % 2 === 1 && i !== 10 ? "true" : undefined}>
                {bandLabel(i, maxScore)}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
