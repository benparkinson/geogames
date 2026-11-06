import { useId } from "react";

export function GuessBox({ value, name, onChange, disabled, className, autoFocus, suggestions }: GuessBoxProps): JSX.Element {
  const listId = useId();

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
        onChange={onChange}
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

export class GuessBoxProps {
  value: string;
  name: string;
  onChange: (e: React.FormEvent<HTMLInputElement>) => void;
  disabled: boolean;
  className?: string;
  autoFocus?: boolean;
  suggestions?: string[];
}
