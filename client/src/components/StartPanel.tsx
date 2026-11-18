import { useState } from "react";
import { useCreateGame } from "../api/games";
import { MenuOption, ROUND_OPTIONS } from "../games/registry";
import { Button, Notice, Spinner, ToggleGroup } from "../ui";
import { useDelayedFlag } from "../ui/useDelayedFlag";
import HardModeToggle from "./HardModeToggle";

/** The chosen game with its round count, Hard mode switch and start button. It creates the game. */
export default function StartPanel({ option }: { option: MenuOption }): React.JSX.Element {
  const { createGame, isLoading, error } = useCreateGame();
  const [rounds, setRounds] = useState(5);
  const slow = useDelayedFlag(isLoading, 3000);

  return (
    <aside className="home-start" aria-label="Start a game">
      <div className="d-flex flex-wrap align-items-center gap-2">
        <div className="d-flex align-items-center gap-2 w-100">
          <option.Icon size={20} aria-hidden="true" />
          <span className="fw-bold">{option.title}</span>
        </div>
        <div className="d-flex align-items-center gap-2">
          <span id="rounds-label" className="fw-semibold">
            Rounds
          </span>
          <ToggleGroup
            size="sm"
            name="rounds"
            value={rounds}
            onChange={setRounds}
            aria-labelledby="rounds-label"
            options={ROUND_OPTIONS.map((n) => ({ value: n, label: n }))}
          />
        </div>
        {option.hasClues && (
          <div className="w-100">
            <HardModeToggle />
          </div>
        )}
        <Button
          variant="primary"
          className="home-start-button flex-grow-1"
          onClick={() => createGame(option.type, rounds)}
          disabled={isLoading}
        >
          {isLoading ? (
            <>
              <Spinner size="sm" decorative /> Starting…
            </>
          ) : (
            "Start game"
          )}
        </Button>
      </div>
      <div aria-live="polite" className="mt-2">
        {isLoading && slow && (
          <p className="gg-text-muted mb-0">Waking up the server. The first game can take up to a minute.</p>
        )}
        {!!error && !isLoading && (
          <Notice className="mb-0">Couldn&apos;t start a game. The server may be down. Try again.</Notice>
        )}
      </div>
    </aside>
  );
}
