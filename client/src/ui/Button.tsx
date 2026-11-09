export type ButtonVariant = "primary" | "secondary" | "hint" | "danger" | "quiet";
export type ButtonSize = "sm" | "md" | "lg";

export interface ButtonStyleOptions {
  variant?: ButtonVariant;
  size?: ButtonSize;
  block?: boolean;
  pill?: boolean;
  className?: string;
}

/** The class string for a button. Use it directly on links and other elements that look like buttons. */
export function buttonClass({ variant = "secondary", size = "md", block, pill, className }: ButtonStyleOptions = {}): string {
  return [
    "gg-btn",
    "gg-btn--" + variant,
    size !== "md" && "gg-btn--" + size,
    block && "gg-btn--block",
    pill && "gg-btn--pill",
    className,
  ].filter(Boolean).join(" ");
}

type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & Omit<ButtonStyleOptions, "className">;

export function Button({ variant, size, block, pill, className, type = "button", ...rest }: ButtonProps): React.JSX.Element {
  return <button type={type} className={buttonClass({ variant, size, block, pill, className })} {...rest} />;
}
