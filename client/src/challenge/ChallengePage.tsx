import Link from "next/link";
import { useChallenge, useCreateChallengeGame } from "../api/challenges";
import { useWakeServer } from "../api/health";
import { creationErrorMessage, hasStatus } from "../api/http";
import LoadingSpinner from "../components/LoadingSpinner";
import PageShell from "../components/PageShell";
import Ticket from "../components/Ticket";
import { DIFFICULTY_LABEL } from "../games/common/difficultyLabel";
import { GAME_NAMES, type GameTypeName } from "../games/names";
import { Button, buttonClass, Notice, Spinner } from "../ui";
import { saveChallengeScore } from "./challengeStorage";

/**
 * A friend's challenge: what they played and what to beat, and a Play button. A game is only made on the click,
 * never on load, because crawlers fetch the page.
 */
export default function ChallengePage({ id }: { id: string | null }): React.JSX.Element {
  const { data, error, isPending } = useChallenge(id);
  const play = useCreateChallengeGame();
  // Start waking a cold server while the player reads.
  useWakeServer();

  if (id === null || (isPending && !error)) {
    return <LoadingSpinner fullPage className="gg-text-correct" />;
  }
  if (!data) {
    const expired = hasStatus(error, 404) || hasStatus(error, 400);
    return (
      <PageShell bare>
        <Ticket
          stripTag="h1"
          label="Challenge"
          date={null}
          footer={
            <Link href="/" className={buttonClass({ variant: "secondary" })}>
              Home
            </Link>
          }
        >
          <h2 className="gg-ticket__heading">
            {expired ? "This challenge has expired" : "Couldn't load the challenge"}
          </h2>
          <p className="gg-daily__text">
            {expired
              ? "Challenges are kept for a week. Ask your friend for a new one, or play a game of your own."
              : "The server may be down. Try again in a moment."}
          </p>
        </Ticket>
      </PageShell>
    );
  }

  const challenge = data;
  const challengeId = id;
  const name = GAME_NAMES[data.gameMode as GameTypeName] ?? data.gameMode;
  const settings = [
    name,
    `${data.roundCount} ${data.roundCount === 1 ? "round" : "rounds"}`,
    ...DIFFICULTY_LABEL[data.difficulty],
  ];

  function start(): void {
    const { score, maxScore } = challenge;
    // Remember the score to beat, for the summary of the new game.
    play.mutate(challengeId, { onSuccess: (game) => saveChallengeScore({ gameId: game.id, score, maxScore }) });
  }

  return (
    <PageShell bare>
      <Ticket
        stripTag="h1"
        label="Challenge"
        date={null}
        footerBar
        footer={
          <Button variant="primary" block onClick={start} disabled={play.isPending}>
            {play.isPending ? (
              <>
                <Spinner size="sm" decorative /> Starting…
              </>
            ) : (
              "Play"
            )}
          </Button>
        }
      >
        <h2 className="gg-ticket__heading">A friend challenged you</h2>
        <p className="gg-daily__text gg-mono">{settings.join(" · ")}</p>
        <p className="gg-daily__text">
          They scored {data.score} / {data.maxScore}. Can you beat it?
        </p>
        <div aria-live="polite" className="gg-daily__status">
          {!!play.error && !play.isPending && (
            <Notice>{creationErrorMessage(play.error, "Couldn't start. The server may be down. Try again.")}</Notice>
          )}
        </div>
      </Ticket>
    </PageShell>
  );
}
