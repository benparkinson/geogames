import { Spinner } from "../ui";

export default function LoadingSpinner({
  fullPage = false,
  className,
}: {
  fullPage?: boolean;
  className?: string;
}): React.JSX.Element {
  const size = fullPage ? "gg-loading--full-page" : "gg-loading--fill";
  return (
    <div className={`gg-loading ${size}${className ? " " + className : ""}`}>
      <Spinner />
    </div>
  );
}
