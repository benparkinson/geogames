import { useId } from "react";

export function GuessBox({ value, name, onChange, disabled, className, autoFocus, suggestions }: GuessBoxProps): React.JSX.Element {
  const listId = useId();

  // On mobile, choosing an option from a native datalist blurs the input and
  // dismisses the keyboard. Detect that change and put the focus back.
  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const input = e.currentTarget;
    const inputType = (e.nativeEvent as InputEvent).inputType;
    const pickedSuggestion =
      !!suggestions &&
      (inputType === "insertReplacementText" || inputType === undefined) &&
      suggestions.includes(input.value);
    onChange(e);
    if (pickedSuggestion) {
      setTimeout(() => input.focus(), 0);
    }
  }

  return (
    <>
      <input
        className={"answer-attempt form-control" + (className ? " " + className : "")}
        type="text"
        autoComplete="off"
        placeholder={name}
        aria-label={name}
        name={name}
        value={value}
        onChange={handleChange}
        disabled={disabled}
        autoFocus={autoFocus}
        list={suggestions ? listId : undefined}
      />
      {suggestions && (
        <datalist id={listId}>
          {suggestions.map(suggestion => <option key={suggestion} value={suggestion} />)}
        </datalist>
      )}
    </>
  );
}

interface GuessBoxProps {
  value: string;
  name: string;
  onChange: (e: React.FormEvent<HTMLInputElement>) => void;
  disabled: boolean;
  className?: string;
  autoFocus?: boolean;
  suggestions?: string[];
}
