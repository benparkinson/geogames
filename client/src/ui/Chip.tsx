export type ChipTone = "correct" | "revealed" | "neutral";

/** A small pill label: a found answer, a revealed answer or a wrong guess. */
export function Chip({ tone, size = "md", strike = false, className, children, ...rest }: {
  tone: ChipTone;
  size?: "sm" | "md";
  strike?: boolean;
  className?: string;
  children: React.ReactNode;
} & Omit<React.HTMLAttributes<HTMLSpanElement>, "className" | "children">): React.JSX.Element {
  const classes = ["gg-chip", `gg-chip--${tone}`, size === "sm" && "gg-chip--sm", strike && "gg-chip--strike", className];
  return <span className={classes.filter(Boolean).join(" ")} {...rest}>{children}</span>;
}
