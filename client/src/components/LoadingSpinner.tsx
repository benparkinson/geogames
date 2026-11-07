import { Spinner } from "react-bootstrap";

export default function LoadingSpinner({ fullPage = false, variant, className }: {
  fullPage?: boolean;
  variant?: string;
  className?: string;
}): React.JSX.Element {
  const size = fullPage ? "min-vh-100" : "h-100";
  return (
    <div className={`d-flex ${size} justify-content-center align-items-center${className ? " " + className : ""}`}>
      <Spinner animation="border" variant={variant} />
    </div>
  );
}
