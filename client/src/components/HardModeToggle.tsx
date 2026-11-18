import { useId } from "react";
import { ToggleGroup } from "../ui";
import { useHardMode } from "../settings/hardMode";

const ON_DESCRIPTION = "No distance or direction clues after a wrong guess.";
const OFF_DESCRIPTION = "You get a distance and direction clue after a wrong guess.";

/** The Hard mode switch, on the start screens only: a game keeps the setting it started with. */
export default function HardModeToggle(): React.JSX.Element {
  const [hardMode, setHardMode] = useHardMode();
  const id = useId();
  const labelId = `${id}-label`;
  const description = hardMode ? ON_DESCRIPTION : OFF_DESCRIPTION;

  return (
    <div className="d-flex flex-wrap align-items-center gap-2" title={description}>
      <span id={labelId} className="fw-semibold">
        Hard mode
      </span>
      <ToggleGroup
        size="sm"
        name={`hard-mode-${id}`}
        aria-labelledby={labelId}
        value={hardMode ? "on" : "off"}
        onChange={(value) => setHardMode(value === "on")}
        options={[
          { value: "off", label: "Off" },
          { value: "on", label: "On" },
        ]}
      />
      <span className="small gg-text-muted">{description}</span>
    </div>
  );
}
