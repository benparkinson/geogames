import Link from "next/link";
import SettingsMenu from "./SettingsMenu";

/** The link to the credits and privacy page and the Settings popup, shown at the foot of every page that isn't a full-screen game. */
function SiteFooter({ pinned = false }: { pinned?: boolean }): React.JSX.Element {
  return (
    <footer className={"gg-footer gg-text-muted" + (pinned ? " gg-footer--pinned" : "")}>
      <Link href="/privacy">Credits and privacy</Link>
      <span aria-hidden="true"> · </span>
      <SettingsMenu />
    </footer>
  );
}

export default SiteFooter;
