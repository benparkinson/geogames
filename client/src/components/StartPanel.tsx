import { useState } from "react";
import { useCreateGame } from "../api/games";
import { creationErrorMessage } from "../api/http";
import { useDifficulty } from "../settings/difficulty";
import { MenuOption, ROUND_OPTIONS } from "../games/registry";
import { Button, Notice, Spinner, ToggleGroup } from "../ui";
import { useDelayedFlag } from "../ui/useDelayedFlag";
import DifficultyToggle from "./DifficultyToggle";

/** The chosen game with its round count, Difficulty control and start button. It creates the game. */
export default function StartPanel({ option }: { option: MenuOption }): React.JSX.Element {
  const { createGame, isLoading, error } = useCreateGame();
  const [difficulty] = useDifficulty();
  const [rounds, setRounds] = useState(5);
  const slow = useDelayedFlag(isLoading, 3000);

  return (
    <aside className="home-start" aria-label="Start a game">
      <div className="home-start__row">
        <div className="home-start__title">
          <option.Icon size={20} aria-hidden="true" />
          <span>{option.title}</span>
        </div>
        <div className="home-start__rounds">
          <span id="rounds-label" className="gg-label">
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
        <div className="home-start__difficulty">
          <DifficultyToggle />
        </div>
        <Button
          variant="primary"
          className="home-start-button"
          // Hard only means something for a game with clues, so any other game starts as Normal.
          onClick={() =>
            createGame(option.type, rounds, difficulty === "HARD" && !option.hasClues ? "NORMAL" : difficulty)
          }
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
      <div aria-live="polite" className="home-start__status">
        {isLoading && slow && (
          <p className="gg-text-muted">Waking up the server. The first game can take up to a minute.</p>
        )}
        {!!error && !isLoading && <Notice>{creationErrorMessage(error)}</Notice>}
      </div>
    </aside>
  );
}
