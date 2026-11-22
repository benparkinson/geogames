import { buttonClass } from "../ui";
import { SupportIcon } from "../ui/icons";

/**
 * A small "Buy me a coffee" link, styled as a quiet outline button so it doesn't compete with
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
      className={buttonClass({ variant: "quiet", size: "sm", pill: true })}
    >
      <SupportIcon aria-hidden="true" className="kofi-icon" /> Buy me a coffee
    </a>
  );
}
