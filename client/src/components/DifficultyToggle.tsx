import { useId } from "react";
import { ToggleGroup } from "../ui";
import type { Difficulty } from "../api/types";
import { useDifficulty } from "../settings/difficulty";

const DESCRIPTIONS: Record<Difficulty, string> = {
  EASY: "The continent is shown and you get more guesses.",
  NORMAL: "A distance and direction clue after each wrong guess.",
  HARD: "No distance or direction clues after a wrong guess.",
};

/** The Difficulty control, on the start screens only: a game keeps the difficulty it started with. */
export default function DifficultyToggle(): React.JSX.Element {
  const [difficulty, setDifficulty] = useDifficulty();
  const id = useId();
  const labelId = `${id}-label`;
  const description = DESCRIPTIONS[difficulty];

  return (
    <div className="gg-difficulty" title={description}>
      <span id={labelId} className="gg-label">
        Difficulty
      </span>
      <ToggleGroup
        size="sm"
        name={`difficulty-${id}`}
        aria-labelledby={labelId}
        value={difficulty}
        onChange={setDifficulty}
        options={[
          { value: "EASY", label: "Easy" },
          { value: "NORMAL", label: "Normal" },
          { value: "HARD", label: "Hard" },
        ]}
      />
      <span className="gg-difficulty__description gg-text-muted">{description}</span>
    </div>
  );
}
