import { useCallback, useEffect, useId, useLayoutEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { prepareSuggestions, rankSuggestions, type Suggestion } from "./suggestions";

const MAX_OPTIONS = 5;
/** Matches `.gg-suggestion`'s min-height in theme/components.css. */
const ROW_HEIGHT = 44;
const LIST_BORDER = 2;
const FULL_LIST_HEIGHT = MAX_OPTIONS * ROW_HEIGHT + LIST_BORDER;
const GAP = 4;
const MARGIN = 8;

export interface ListPlacement {
  direction: "up" | "down";
  left: number;
  width: number;
  /** For `position: fixed`, measured from the top of the layout viewport (direction "down"). */
  top?: number;
  /** Measured from the bottom of the layout viewport (direction "up"). */
  bottom?: number;
  maxHeight: number;
}

/**
 * Where to draw the list: below the input when its full height fits there, otherwise on whichever side has more
 * room. `viewportHeight` is the visible height (the visual viewport, so a phone's keyboard is excluded).
 */
export function placeList(
  input: Pick<DOMRect, "top" | "bottom" | "left" | "width">,
  viewportHeight: number,
  listHeight: number = FULL_LIST_HEIGHT,
  layoutHeight: number = viewportHeight,
): ListPlacement {
  const below = viewportHeight - input.bottom;
  const above = input.top;
  const down = below >= listHeight + GAP + MARGIN || below >= above;
  const base = { left: input.left, width: input.width };
  if (down) {
    return { ...base, direction: "down", top: input.bottom + GAP, maxHeight: Math.max(0, below - MARGIN) };
  }
  return {
    ...base,
    direction: "up",
    bottom: layoutHeight - input.top + GAP,
    maxHeight: Math.max(0, above - MARGIN),
  };
}

function measure(input: HTMLInputElement): ListPlacement {
  const viewportHeight = window.visualViewport?.height ?? window.innerHeight;
  return placeList(input.getBoundingClientRect(), viewportHeight, FULL_LIST_HEIGHT, window.innerHeight);
}

export function GuessBox({
  value,
  name,
  onChange,
  disabled,
  className,
  autoFocus,
  suggestions,
  ref,
}: GuessBoxProps): React.JSX.Element {
  const listId = useId();
  const [input, setInput] = useState<HTMLInputElement | null>(null);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState<number | null>(null);
  const [placement, setPlacement] = useState<ListPlacement | null>(null);

  const prepared = useMemo(() => prepareSuggestions(suggestions ?? []), [suggestions]);
  const matches = useMemo(
    () => (suggestions ? rankSuggestions(value, prepared, MAX_OPTIONS) : []),
    [suggestions, value, prepared],
  );
  const visible = open && !disabled && matches.length > 0;
  const direction = placement?.direction ?? "down";

  // Our own callback ref: the parent's ref and the one we measure with.
  const setRefs = useCallback(
    (el: HTMLInputElement | null) => {
      setInput(el);
      if (typeof ref === "function") ref(el);
      else if (ref) ref.current = el;
    },
    [ref],
  );

  useLayoutEffect(() => {
    if (!visible || !input) return;
    const update = () => setPlacement(measure(input));
    update();
    const viewport = window.visualViewport;
    window.addEventListener("resize", update);
    viewport?.addEventListener("resize", update);
    viewport?.addEventListener("scroll", update);
    document.addEventListener("scroll", update, true);
    return () => {
      window.removeEventListener("resize", update);
      viewport?.removeEventListener("resize", update);
      viewport?.removeEventListener("scroll", update);
      document.removeEventListener("scroll", update, true);
    };
  }, [visible, input, matches.length]);

  // iOS Safari keeps the focus (and the keyboard) in the box when the player taps a part of the page that can't take
  // the focus, such as the map, so no blur closes the list. Close it on any press outside the box and the list,
  // caught in the capture phase because Leaflet stops some presses from propagating.
  useEffect(() => {
    if (!visible) return;
    const closeOutside = (e: PointerEvent) => {
      const target = e.target as Node | null;
      if (target && (input?.contains(target) || document.getElementById(listId)?.contains(target))) return;
      setOpen(false);
      setActive(null);
    };
    document.addEventListener("pointerdown", closeOutside, true);
    return () => document.removeEventListener("pointerdown", closeOutside, true);
  }, [visible, input, listId]);

  // The Guess button keeps the focus in the box, so the box never sees the submit: listen on the form.
  useEffect(() => {
    const form = input?.form;
    if (!form) return;
    const close = () => setOpen(false);
    form.addEventListener("submit", close);
    return () => form.removeEventListener("submit", close);
  }, [input]);

  function pick(match: string) {
    onChange(match);
    setOpen(false);
    setActive(null);
    input?.focus();
  }

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setOpen(true);
    setActive(null);
    onChange(e.currentTarget.value);
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (!visible) return;
    // Away from the box is down the screen when the list opens down, and up the screen when it opens up.
    const away = direction === "down" ? "ArrowDown" : "ArrowUp";
    const back = direction === "down" ? "ArrowUp" : "ArrowDown";
    if (e.key === away) {
      e.preventDefault();
      setActive(active === null ? 0 : Math.min(active + 1, matches.length - 1));
    } else if (e.key === back) {
      e.preventDefault();
      setActive(active === null || active === 0 ? null : active - 1);
    } else if (e.key === "Enter" && active !== null) {
      e.preventDefault();
      pick(matches[active].name);
    } else if (e.key === "Escape") {
      e.preventDefault();
      setOpen(false);
      setActive(null);
    }
  }

  return (
    <>
      <input
        ref={setRefs}
        className={"answer-attempt gg-input" + (className ? " " + className : "")}
        type="text"
        autoComplete="off"
        placeholder={name}
        aria-label={name}
        name={name}
        value={value}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        onBlur={() => setOpen(false)}
        disabled={disabled}
        autoFocus={autoFocus}
        {...(suggestions
          ? {
              role: "combobox",
              "aria-autocomplete": "list" as const,
              "aria-expanded": visible,
              "aria-controls": listId,
              "aria-activedescendant": visible && active !== null ? `${listId}-${active}` : undefined,
            }
          : {})}
      />
      {visible &&
        placement &&
        createPortal(
          <ul
            id={listId}
            role="listbox"
            aria-label={name}
            className={"gg-suggestions gg-suggestions-" + placement.direction}
            style={{
              left: placement.left,
              width: placement.width,
              top: placement.top,
              bottom: placement.bottom,
              maxHeight: placement.maxHeight,
            }}
          >
            {matches.map((match, index) => (
              <li
                key={match.name}
                id={`${listId}-${index}`}
                role="option"
                aria-selected={index === active}
                className={"gg-suggestion" + (index === active ? " gg-suggestion-active" : "")}
                // Keep the focus in the box, so a phone's keyboard stays up; the pick happens on click.
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => pick(match.name)}
              >
                {match.name}
                {match.alias && <span className="gg-suggestion-alias">{match.alias}</span>}
              </li>
            ))}
          </ul>,
          document.body,
        )}
    </>
  );
}

interface GuessBoxProps {
  value: string;
  name: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  className?: string;
  autoFocus?: boolean;
  suggestions?: Suggestion[];
  ref?: React.Ref<HTMLInputElement>;
}
