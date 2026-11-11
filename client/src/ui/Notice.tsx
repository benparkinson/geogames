export function Notice({ tone = "danger", className, children }: {
  tone?: "danger";
  className?: string;
  children: React.ReactNode;
}): React.JSX.Element {
  return <div role="alert" className={`gg-notice gg-notice--${tone}${className ? " " + className : ""}`}>{children}</div>;
}
