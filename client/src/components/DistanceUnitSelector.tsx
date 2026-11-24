import { useId } from "react";
import { ToggleGroup } from "../ui";
import { useDistanceUnit } from "../settings/units";

/** The Distances control: kilometres or miles for the wrong-guess clue. */
export default function DistanceUnitSelector(): React.JSX.Element {
  const [unit, setUnit] = useDistanceUnit();
  const id = useId();
  const labelId = `${id}-label`;

  return (
    <div className="gg-difficulty">
      <span id={labelId} className="gg-label">
        Distances
      </span>
      <ToggleGroup
        size="sm"
        name={`units-${id}`}
        aria-labelledby={labelId}
        value={unit}
        onChange={setUnit}
        options={[
          { value: "km", label: "Kilometres" },
          { value: "mi", label: "Miles" },
        ]}
      />
    </div>
  );
}
