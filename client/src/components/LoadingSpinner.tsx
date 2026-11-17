import { Spinner } from "../ui";

export default function LoadingSpinner({ fullPage = false, className }: {
  fullPage?: boolean;
  className?: string;
}): React.JSX.Element {
  const size = fullPage ? "min-vh-100" : "h-100";
  return (
    <div className={`d-flex ${size} justify-content-center align-items-center${className ? " " + className : ""}`}>
      <Spinner />
    </div>
  );
}
