import { useWakeServer } from "../api/health";
import { creationErrorMessage } from "../api/http";
import PageShell from "../components/PageShell";
import DifficultyToggle from "../components/DifficultyToggle";
import Ticket, { ticketDate } from "../components/Ticket";
import { Button, Notice, Spinner } from "../ui";
import { useDelayedFlag } from "../ui/useDelayedFlag";
import { useNowSeconds } from "./useNowSeconds";
import { useStartDaily } from "./useStartDaily";

export default function DailyIntro(): React.JSX.Element {
  const { play, isPending, error } = useStartDaily();
  const slow = useDelayedFlag(isPending, 3000);
  const nowSeconds = useNowSeconds();
  // Start waking a cold server while the player reads.
  useWakeServer();

  return (
    <PageShell bare>
      <Ticket
        stripTag="h1"
        date={nowSeconds === null ? null : ticketDate(new Date(nowSeconds * 1000))}
        footer={
          <Button variant="primary" className="gg-ticket__play" onClick={play} disabled={isPending}>
            {isPending ? (
              <>
                <Spinner size="sm" decorative /> Starting…
              </>
            ) : (
              "Play today's challenge"
            )}
          </Button>
        }
      >
        <h2 className="gg-ticket__heading">Today&apos;s five</h2>
        <p className="gg-daily__text">
          Five geography puzzles from a variety of game modes. Wrong answers don&apos;t lower your score, but hints do.
        </p>
        <div className="gg-daily__block">
          <DifficultyToggle />
        </div>
        <div aria-live="polite" className="gg-daily__status">
          {isPending && slow && <p className="gg-text-muted">Waking up the server…</p>}
          {!!error && !isPending && (
            <Notice>{creationErrorMessage(error, "Couldn't start. The server may be down. Try again.")}</Notice>
          )}
        </div>
      </Ticket>
    </PageShell>
  );
}
