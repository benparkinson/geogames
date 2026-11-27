import Link from "next/link";
import GameLinks from "./GameLinks";

/**
 * The foot of every page that isn't a full-screen game: the link to the credits and privacy page and the list of
 * games' landing pages. The list is in the HTML while closed, so search engines follow it.
 */
function SiteFooter({ pinned = false }: { pinned?: boolean }): React.JSX.Element {
  return (
    <footer className={"gg-footer gg-text-muted" + (pinned ? " gg-footer--pinned" : "")}>
      <Link href="/privacy">Credits and privacy</Link>
      <details className="gg-footer__games">
        <summary>Games</summary>
        <GameLinks className="gg-footer__list" />
      </details>
    </footer>
  );
}

export default SiteFooter;
