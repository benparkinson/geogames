import { useWakeServer } from "../api/health";
import PageShell from "../components/PageShell";
import DifficultyToggle from "../components/DifficultyToggle";
import { Button, Notice, Spinner } from "../ui";
import { useDelayedFlag } from "../ui/useDelayedFlag";
import { useStartDaily } from "./useStartDaily";

export default function DailyIntro(): React.JSX.Element {
  const { play, isPending, error } = useStartDaily();
  const slow = useDelayedFlag(isPending, 3000);
  // Start waking a cold server while the player reads.
  useWakeServer();

  return (
    <PageShell>
      <h1 className="h3 mb-1">Daily challenge</h1>
      <p className="mb-3">
        Five geography puzzles from a variety of game modes. Wrong answers don&apos;t lower your score, but hints do.
      </p>
      <div className="mb-3">
        <DifficultyToggle />
      </div>
      <Button variant="primary" onClick={play} disabled={isPending}>
        {isPending ? (
          <>
            <Spinner size="sm" decorative /> Starting…
          </>
        ) : (
          "Play today's challenge"
        )}
      </Button>
      <div aria-live="polite" className="mt-2">
        {isPending && slow && <p className="gg-text-muted mb-0">Waking up the server…</p>}
        {!!error && !isPending && (
          <Notice className="mb-0">Couldn&apos;t start. The server may be down. Try again.</Notice>
        )}
      </div>
    </PageShell>
  );
}
