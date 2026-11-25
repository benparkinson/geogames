import Link from "next/link";

/** The link to the credits and privacy page, shown at the foot of every page that isn't a full-screen game. */
function SiteFooter({ pinned = false }: { pinned?: boolean }): React.JSX.Element {
  return (
    <footer className={"gg-footer gg-text-muted" + (pinned ? " gg-footer--pinned" : "")}>
      <Link href="/privacy">Credits and privacy</Link>
    </footer>
  );
}

export default SiteFooter;
