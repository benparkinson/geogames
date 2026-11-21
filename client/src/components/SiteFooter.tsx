import Link from "next/link";

/** The one link to the credits and privacy page, shown at the foot of every page that isn't a full-screen game. */
function SiteFooter({ className = "mt-4" }: { className?: string }): React.JSX.Element {
  return (
    <footer className={"small gg-text-muted text-center " + className}>
      <Link href="/privacy">Credits and privacy</Link>
    </footer>
  );
}

export default SiteFooter;
