import { useRef } from "react";
import { MENU_OPTIONS, MenuOption } from "../games/registry";
import { ExtraDifficultIcon } from "../ui/icons";

const STANDARD_OPTIONS = MENU_OPTIONS.filter((option) => !option.extraDifficult);
const EXTRA_DIFFICULT_OPTIONS = MENU_OPTIONS.filter((option) => option.extraDifficult);

/**
 * The games to choose from, as ruled rows in one radio group across both sections, so the arrow keys move through
 * every row. `renderPanel` draws the start panel directly under the selected row.
 */
export default function GameCardGrid({
  selected,
  onSelect,
  renderPanel,
}: {
  selected: string | null;
  onSelect: (type: string) => void;
  renderPanel: (option: MenuOption) => React.ReactNode;
}): React.JSX.Element {
  const rowRefs = useRef<(HTMLButtonElement | null)[]>([]);

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
    rowRefs.current[next]?.focus();
  }

  function renderRow(option: MenuOption): React.JSX.Element {
    // The position in the full list, which the arrow keys, focus refs and row numbers use.
    const index = MENU_OPTIONS.indexOf(option);
    const isSelected = option.type === selected;
    return (
      <div key={option.type} className="game-row" data-selected={isSelected}>
        <button
          type="button"
          role="radio"
          aria-checked={isSelected}
          tabIndex={(selected === null ? index === 0 : isSelected) ? 0 : -1}
          ref={(el) => {
            rowRefs.current[index] = el;
          }}
          className="game-row__button"
          onClick={() => onSelect(option.type)}
          onKeyDown={(e) => handleKeyDown(e, index)}
        >
          <span className="game-row__number gg-mono" aria-hidden="true">
            {String(index + 1).padStart(2, "0")}
          </span>
          <option.Icon size={24} aria-hidden="true" />
          <span className="game-row__text">
            <span className="game-row__title">{option.title}</span>
            <span className="game-row__description gg-text-muted">{option.description}</span>
          </span>
        </button>
        {isSelected && renderPanel(option)}
      </div>
    );
  }

  return (
    <div role="radiogroup" aria-label="Choose a game" className="game-picker">
      <h2 className="game-picker__heading">Practice</h2>
      <div className="game-picker__list">{STANDARD_OPTIONS.map(renderRow)}</div>
      <div role="group" aria-labelledby="extra-difficult-label" className="game-picker__extra">
        <h2 id="extra-difficult-label" className="game-picker__heading">
          <ExtraDifficultIcon className="gg-text-danger" aria-hidden="true" /> Extra difficult
        </h2>
        <p className="game-picker__note gg-text-muted">Just a shape on a blank map, with nothing around it.</p>
        <div className="game-picker__list">{EXTRA_DIFFICULT_OPTIONS.map(renderRow)}</div>
      </div>
    </div>
  );
}
