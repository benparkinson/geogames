import { CupHotFill } from "react-bootstrap-icons";

/**
 * A small "Support me on Ko-fi" link, styled as a quiet outline button so it doesn't compete with
 * the Start button. It stands in for the Ko-fi widget (same text and profile) as plain markup,
 * because the widget script uses document.write, which doesn't work inside React and would load a
 * third-party script on every page.
 */
export default function KofiBadge(): React.JSX.Element {
    return (
        <a
            href="https://ko-fi.com/O3J02ABGOW"
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-sm btn-outline-secondary rounded-pill d-inline-flex align-items-center gap-1"
        >
            <CupHotFill aria-hidden="true" style={{ color: "#72a4f2" }} /> Support me on Ko-fi
        </a>
    );
}
