export function GuessBox({ value, name, onChange, disabled, className, autoFocus }: GuessBoxProps): JSX.Element {
  return (
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
    />
  );
}

export class GuessBoxProps {
  value: string;
  name: string;
  onChange: (e: React.FormEvent<HTMLInputElement>) => void;
  disabled: boolean;
  className?: string;
  autoFocus?: boolean;
}
