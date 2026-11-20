import { useId } from "react";
import { ToggleGroup } from "../ui";
import type { Difficulty } from "../api/types";
import { useDifficulty } from "../settings/difficulty";

const DESCRIPTIONS: Record<Difficulty, string> = {
  EASY: "The continent is shown and you get twice the guesses.",
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
    <div className="d-flex flex-wrap align-items-center gap-2" title={description}>
      <span id={labelId} className="fw-semibold">
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
      <span className="small gg-text-muted">{description}</span>
    </div>
  );
}
