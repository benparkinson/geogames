import { useEffect, useRef } from "react";
import { MENU_OPTIONS, MenuOption } from "../games/registry";
import { ExtraDifficultIcon } from "../ui/icons";

const STANDARD_OPTIONS = MENU_OPTIONS.filter((option) => !option.extraDifficult);
const EXTRA_DIFFICULT_OPTIONS = MENU_OPTIONS.filter((option) => option.extraDifficult);

/**
 * The games to choose from, as ruled rows. Each row is a button that opens its start panel (`renderPanel`)
 * directly under it, and closes it when pressed again; one row is open at a time. The arrow keys move the focus
 * through every row of both sections without opening anything, so the panel doesn't jump while the player looks.
 * The rows are disclosure buttons rather than a radio group because the open panel holds radios of its own.
 */
export default function GameCardGrid({
  selected,
  onSelect,
  renderPanel,
}: {
  selected: string | null;
  onSelect: (type: string | null) => void;
  renderPanel: (option: MenuOption) => React.ReactNode;
}): React.JSX.Element {
  const rowRefs = useRef<(HTMLButtonElement | null)[]>([]);

  // A panel that opens below the screen's edge (a phone, or the sticky bottom bar in the way) is scrolled into view.
  useEffect(() => {
    if (selected) document.getElementById(`start-${selected}`)?.scrollIntoView?.({ block: "nearest" });
  }, [selected]);

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
    rowRefs.current[next]?.focus();
  }

  function renderRow(option: MenuOption): React.JSX.Element {
    // The position in the full list, which the arrow keys, focus refs and row numbers use.
    const index = MENU_OPTIONS.indexOf(option);
    const isSelected = option.type === selected;
    const panelId = `start-${option.type}`;
    return (
      <div key={option.type} className="game-row" data-selected={isSelected}>
        <button
          type="button"
          aria-expanded={isSelected}
          aria-controls={isSelected ? panelId : undefined}
          ref={(el) => {
            rowRefs.current[index] = el;
          }}
          className="game-row__button"
          onClick={() => onSelect(isSelected ? null : option.type)}
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
        {isSelected && (
          <div id={panelId} className="game-row__panel">
            {renderPanel(option)}
          </div>
        )}
      </div>
    );
  }

  return (
    <div role="group" aria-label="Choose a game" className="game-picker">
      <h2 className="game-picker__heading">Practice</h2>
      <div className="game-picker__list">{STANDARD_OPTIONS.map(renderRow)}</div>
      <div role="group" aria-labelledby="extra-difficult-label" className="game-picker__extra">
        <h2 id="extra-difficult-label" className="game-picker__heading">
          <ExtraDifficultIcon className="gg-text-danger" aria-hidden="true" /> Extra difficult
        </h2>
        <div className="game-picker__list">{EXTRA_DIFFICULT_OPTIONS.map(renderRow)}</div>
      </div>
    </div>
  );
}
