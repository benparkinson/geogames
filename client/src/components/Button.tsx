export function Button({ bootstrapClass, text, onClick, disabled }: ButtonProps): JSX.Element {
  return (
    <button type="button" className={"btn " + bootstrapClass} onClick={onClick} disabled={disabled}>
      {text}
    </button>
  );
}

export class ButtonProps {
  bootstrapClass: string;
  text: string;
  onClick: (e: React.MouseEvent<HTMLInputElement>) => void;
  disabled?: boolean;
}
