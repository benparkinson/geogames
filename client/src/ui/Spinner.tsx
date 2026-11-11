/** A spinning ring in the current text colour. Decorative inside a control that already says what is happening. */
export function Spinner({ size = "md", decorative = false }: { size?: "sm" | "md"; decorative?: boolean }): React.JSX.Element {
  const className = "gg-spinner" + (size === "sm" ? " gg-spinner--sm" : "");
  if (decorative) {
    return <span className={className} aria-hidden="true" />;
  }
  return (
    <span className={className} role="status">
      <span className="gg-visually-hidden">Loading…</span>
    </span>
  );
}
