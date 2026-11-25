import { useState } from "react";
import PageShell from "../components/PageShell";
import Ticket, { ticketDate } from "../components/Ticket";
import { localDateString } from "../api/daily";
import { Button, buttonClass } from "../ui";
import DailyComparison from "./DailyComparison";
import ShareButton from "./ShareButton";
import { formatCountdown, secondsToNextDaily } from "./countdown";
import DailyScoreReveal from "./DailyScoreReveal";
import { DailyEntry, writeDaily } from "./dailyStorage";
import { shareText } from "./shareText";
import { useNowSeconds } from "./useNowSeconds";

export default function DailyFinished({
  entry,
  onOpenGame,
}: {
  entry: DailyEntry & { result: NonNullable<DailyEntry["result"]> };
  onOpenGame: () => void;
}): React.JSX.Element {
  // Shown once: `revealSeen` is stored when it closes. An older result with no bars has nothing to reveal.
  const [revealOpen, setRevealOpen] = useState(() => entry.revealSeen !== true && entry.result.parts !== undefined);
  const closeReveal = () => {
    setRevealOpen(false);
    writeDaily({ ...entry, revealSeen: true });
  };
  const text = shareText({ date: entry.date, difficulty: entry.difficulty, result: entry.result });
  const nowSeconds = useNowSeconds();
  const now = nowSeconds === null ? null : new Date(nowSeconds * 1000);
  // The date of the finished game, from its stored `YYYY-MM-DD`.
  const [year, month, day] = entry.date.split("-").map(Number);
  const footer =
    now === null ? null : localDateString(now) > entry.date ? (
      <>
        <span>Today&apos;s challenge is ready</span>
        {/* A full page load, so DailyPage reads the new date. */}
        {/* eslint-disable-next-line @next/next/no-html-link-for-pages */}
        <a href="/daily" className={buttonClass({ variant: "primary", size: "sm", className: "gg-ticket__play" })}>
          Play it
        </a>
      </>
    ) : (
      <span className="gg-mono" data-countdown>
        Next daily in {formatCountdown(secondsToNextDaily(now))}
      </span>
    );
  return (
    <PageShell bare>
      {revealOpen && entry.result.parts && (
        <DailyScoreReveal
          date={entry.date}
          difficulty={entry.difficulty}
          result={entry.result}
          parts={entry.result.parts}
          onClose={closeReveal}
        />
      )}
      <Ticket stripTag="h1" date={ticketDate(new Date(year, month - 1, day))} footer={footer}>
        <h2 className="gg-ticket__heading">Daily {entry.date}</h2>
        <p className="gg-daily__text">
          You scored {entry.result.score} / {entry.result.maxScore}
        </p>
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
      </Ticket>
    </PageShell>
  );
}
