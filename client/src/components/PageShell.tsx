import Link from "next/link";
import HomeLink from "./HomeLink";
import SiteFooter from "./SiteFooter";
import ThemeToggle from "./ThemeToggle";

/**
 * The frame of a page with one card on it: the top bar (home link and theme switch), the card and the footer.
 * `topBarEnd` goes at the far end of the top bar; `wide` makes the card wider; `compactFooter` swaps the full
 * credits for one link to the privacy page, which carries them, so a short page fits a phone screen.
 */
export default function PageShell({
  children,
  topBarEnd,
  wide = false,
  compactFooter = false,
}: {
  children: React.ReactNode;
  topBarEnd?: React.ReactNode;
  wide?: boolean;
  compactFooter?: boolean;
}): React.JSX.Element {
  return (
    <div className="min-vh-100 page-bg d-flex flex-column">
      {topBarEnd ? (
        <div className="px-3 py-2 d-flex justify-content-between align-items-center">
          <div className="d-flex align-items-center gap-2">
            <HomeLink />
            <ThemeToggle />
          </div>
          {topBarEnd}
        </div>
      ) : (
        <div className="px-3 py-2 d-flex align-items-center gap-2">
          <HomeLink />
          <ThemeToggle />
        </div>
      )}
      <div className={"gg-card" + (wide ? " gg-card--wide" : "") + " mx-auto my-4 p-3"}>{children}</div>
      {compactFooter ? (
        <footer className="small gg-text-muted text-center mt-auto pt-3 pb-3">
          <Link href="/privacy">Credits and privacy</Link>
        </footer>
      ) : (
        <SiteFooter className="mt-auto pt-4 pb-4" />
      )}
    </div>
  );
}
