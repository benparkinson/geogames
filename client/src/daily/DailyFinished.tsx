import PageShell from "../components/PageShell";
import { localDateString } from "../api/daily";
import { Button, buttonClass } from "../ui";
import DailyComparison from "./DailyComparison";
import ShareButton from "./ShareButton";
import { formatCountdown, secondsToNextDaily } from "./countdown";
import { DailyEntry } from "./dailyStorage";
import { shareText } from "./shareText";
import { useNowSeconds } from "./useNowSeconds";

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
  const nowSeconds = useNowSeconds();
  const now = nowSeconds === null ? null : new Date(nowSeconds * 1000);
  return (
    <PageShell>
      <h1 className="gg-daily__title">Daily {entry.date}</h1>
      <p className="gg-daily__text">
        You scored {entry.result.score} / {entry.result.maxScore}
      </p>
      {now !== null &&
        (localDateString(now) > entry.date ? (
          <p className="gg-daily__text">
            Today&apos;s challenge is ready {/* A full page load, so DailyPage reads the new date. */}
            {/* eslint-disable-next-line @next/next/no-html-link-for-pages */}
            <a href="/daily" className={buttonClass({ variant: "primary", size: "sm" })}>
              Play it
            </a>
          </p>
        ) : (
          <p className="gg-daily__text" data-countdown>
            Next daily in {formatCountdown(secondsToNextDaily(now))}
          </p>
        ))}
      <DailyComparison date={entry.date} difficulty={entry.difficulty} score={entry.result.score} />
      <pre className="gg-daily__block" aria-label="Your result">
        {text}
      </pre>
      <div className="gg-daily__actions">
        <ShareButton text={text} />
        <Button variant="secondary" onClick={onOpenGame}>
          See the rounds
        </Button>
      </div>
    </PageShell>
  );
}
