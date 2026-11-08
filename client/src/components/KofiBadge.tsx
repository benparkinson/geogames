import { CupHotFill } from "react-bootstrap-icons";

/**
 * A small "Support me on Ko-fi" link. This is the Ko-fi widget's own output (same text, colour and
 * profile) written as plain markup, because the widget script uses document.write, which doesn't
 * work inside React and would load a third-party script on every page.
 */
export default function KofiBadge(): React.JSX.Element {
    return (
        <a
            href="https://ko-fi.com/O3J02ABGOW"
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-sm text-white rounded-pill d-inline-flex align-items-center gap-1"
            style={{ backgroundColor: "#72a4f2" }}
        >
            <CupHotFill aria-hidden="true" /> Support me on Ko-fi
        </a>
    );
}
