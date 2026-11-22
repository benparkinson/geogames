import { useRef } from "react";
import { MENU_OPTIONS, MenuOption } from "../games/registry";
import { ExtraDifficultIcon } from "../ui/icons";

const STANDARD_OPTIONS = MENU_OPTIONS.filter((option) => !option.extraDifficult);
const EXTRA_DIFFICULT_OPTIONS = MENU_OPTIONS.filter((option) => option.extraDifficult);

/** The games to choose from, as one radio group across both sections, so the arrow keys move through every card. */
export default function GameCardGrid({
  selected,
  onSelect,
}: {
  selected: string | null;
  onSelect: (type: string) => void;
}): React.JSX.Element {
  const cardRefs = useRef<(HTMLButtonElement | null)[]>([]);

  function handleKeyDown(e: React.KeyboardEvent, index: number): void {
    let next: number;
    if (e.key === "ArrowRight" || e.key === "ArrowDown") {
      next = (index + 1) % MENU_OPTIONS.length;
    } else if (e.key === "ArrowLeft" || e.key === "ArrowUp") {
      next = (index - 1 + MENU_OPTIONS.length) % MENU_OPTIONS.length;
    } else {
      return;
    }
    e.preventDefault();
    onSelect(MENU_OPTIONS[next].type);
    cardRefs.current[next]?.focus();
  }

  function renderCard(option: MenuOption): React.JSX.Element {
    // The position in the full list, which the arrow keys and focus refs use.
    const index = MENU_OPTIONS.indexOf(option);
    const isSelected = option.type === selected;
    return (
      <button
        key={option.type}
        type="button"
        role="radio"
        aria-checked={isSelected}
        tabIndex={(selected === null ? index === 0 : isSelected) ? 0 : -1}
        ref={(el) => {
          cardRefs.current[index] = el;
        }}
        className={"game-card" + (isSelected ? " game-card--selected" : "")}
        onClick={() => onSelect(option.type)}
        onKeyDown={(e) => handleKeyDown(e, index)}
      >
        <option.Icon size={28} aria-hidden="true" />
        <div className="game-card__title">{option.title}</div>
        <div className="game-card__description gg-text-muted">{option.description}</div>
      </button>
    );
  }

  return (
    <div role="radiogroup" aria-label="Choose a game" className="game-picker">
      <div className="game-picker__grid">{STANDARD_OPTIONS.map(renderCard)}</div>
      <div role="group" aria-labelledby="extra-difficult-label" className="game-picker__extra">
        <h3 id="extra-difficult-label" className="game-picker__heading">
          <ExtraDifficultIcon className="gg-text-danger" aria-hidden="true" /> Extra difficult
        </h3>
        <p className="game-picker__note gg-text-muted">Just a shape on a blank map, with nothing around it.</p>
        <div className="game-picker__grid">{EXTRA_DIFFICULT_OPTIONS.map(renderCard)}</div>
      </div>
    </div>
  );
}
