import { useId } from "react";
import { ToggleGroup } from "../ui";
import { useThemeChoice } from "../settings/theme";

/** The Appearance control: System default, Light or Dark. */
export default function ThemeSelector(): React.JSX.Element {
  const [choice, setChoice] = useThemeChoice();
  const id = useId();
  const labelId = `${id}-label`;

  return (
    <div className="gg-difficulty">
      <span id={labelId} className="gg-label">
        Appearance
      </span>
      <ToggleGroup
        size="sm"
        name={`theme-${id}`}
        aria-labelledby={labelId}
        value={choice}
        onChange={setChoice}
        options={[
          { value: "system", label: "System default" },
          { value: "light", label: "Light" },
          { value: "dark", label: "Dark" },
        ]}
      />
    </div>
  );
}
