import { Fragment } from "react";
import { buttonClass, ButtonSize } from "./Button";

/** A radio group drawn as joined buttons. The inputs are real radios, so arrow keys and screen readers work natively. */
export function ToggleGroup<T extends string | number>({ name, value, options, onChange, size = "md", "aria-labelledby": labelledBy }: {
  name: string;
  value: T;
  options: { value: T; label: React.ReactNode }[];
  onChange: (value: T) => void;
  size?: ButtonSize;
  "aria-labelledby"?: string;
}): React.JSX.Element {
  return (
    <div role="group" className="gg-toggle" aria-labelledby={labelledBy}>
      {options.map(option => {
        const id = `${name}-${option.value}`;
        return (
          <Fragment key={option.value}>
            <input
              type="radio"
              className="gg-toggle__input"
              id={id}
              name={name}
              checked={option.value === value}
              onChange={() => onChange(option.value)}
            />
            <label className={buttonClass({ variant: "secondary", size })} htmlFor={id}>{option.label}</label>
          </Fragment>
        );
      })}
    </div>
  );
}
