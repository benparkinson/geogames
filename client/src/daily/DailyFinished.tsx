import PageShell from "../components/PageShell";
import { Button } from "../ui";
import DailyComparison from "./DailyComparison";
import ShareButton from "./ShareButton";
import { DailyEntry } from "./dailyStorage";
import { shareText } from "./shareText";

export default function DailyFinished({
  entry,
  onOpenGame,
}: {
  entry: DailyEntry & { result: NonNullable<DailyEntry["result"]> };
  onOpenGame: () => void;
}): React.JSX.Element {
  const text = shareText(
    { date: entry.date, difficulty: entry.difficulty, result: entry.result },
    window.location.origin,
  );
  return (
    <PageShell>
      <h1 className="h3 mb-1">Daily {entry.date}</h1>
      <p className="mb-3">
        You scored {entry.result.score} / {entry.result.maxScore}
      </p>
      <DailyComparison date={entry.date} difficulty={entry.difficulty} score={entry.result.score} />
      <pre className="mb-3" aria-label="Your result">
        {text}
      </pre>
      <div className="d-flex flex-wrap gap-2">
        <ShareButton text={text} />
        <Button variant="secondary" onClick={onOpenGame}>
          See the rounds
        </Button>
      </div>
    </PageShell>
  );
}
