export function Button({ bootstrapClass, text, onClick }: ButtonProps): JSX.Element {
  return (
    <button type="button" className={"btn " + bootstrapClass} onClick={onClick}>
      {text}
    </button>
  );
}

export class ButtonProps {
  bootstrapClass: string;
  text: string;
  onClick: (e: React.MouseEvent<HTMLInputElement>) => void;
}
