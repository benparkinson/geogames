import { useId } from "react";
import { ToggleGroup } from "../ui";
import { useHardcoreMode } from "../settings/hardcoreMode";

const DESCRIPTION = "No distance or direction clues after a wrong guess.";

/** The Hardcore mode switch. `compact` is the small form for the game panel. */
export default function HardcoreToggle({ compact = false }: { compact?: boolean }): React.JSX.Element {
  const [hardcore, setHardcore] = useHardcoreMode();
  const id = useId();
  const labelId = `${id}-label`;

  return (
    <div className="d-flex flex-wrap align-items-center gap-2" title={DESCRIPTION}>
      <span id={labelId} className={compact ? "small fw-semibold" : "fw-semibold"}>
        {compact ? "Hardcore" : "Hardcore mode"}
      </span>
      <ToggleGroup
        size="sm"
        name={`hardcore-${id}`}
        aria-labelledby={labelId}
        value={hardcore ? "on" : "off"}
        onChange={value => setHardcore(value === "on")}
        options={[
          { value: "off", label: "Off" },
          { value: "on", label: "On" },
        ]}
      />
      {!compact && <span className="small gg-text-muted">{DESCRIPTION}</span>}
    </div>
  );
}
