import { useId } from "react";
import { ToggleGroup } from "../ui";
import { useHardMode } from "../settings/hardMode";

const DESCRIPTION = "No distance or direction clues after a wrong guess.";

/** The Hard mode switch. `compact` is the small form for the game panel. */
export default function HardModeToggle({ compact = false }: { compact?: boolean }): React.JSX.Element {
  const [hardMode, setHardMode] = useHardMode();
  const id = useId();
  const labelId = `${id}-label`;

  return (
    <div className="d-flex flex-wrap align-items-center gap-2" title={DESCRIPTION}>
      <span id={labelId} className={compact ? "small fw-semibold" : "fw-semibold"}>
        Hard mode
      </span>
      <ToggleGroup
        size="sm"
        name={`hard-mode-${id}`}
        aria-labelledby={labelId}
        value={hardMode ? "on" : "off"}
        onChange={value => setHardMode(value === "on")}
        options={[
          { value: "off", label: "Off" },
          { value: "on", label: "On" },
        ]}
      />
      {compact ? <span className="small gg-text-muted">No distance clues</span> : <span className="small gg-text-muted">{DESCRIPTION}</span>}
    </div>
  );
}
