import HomeLink from "./HomeLink";
import SiteFooter from "./SiteFooter";
import ThemeToggle from "./ThemeToggle";

/**
 * The frame of a page with one card on it: the top bar (home link and theme switch), the card and the footer.
 * `topBarEnd` goes at the far end of the top bar; `wide` makes the card wider.
 */
export default function PageShell({
  children,
  topBarEnd,
  wide = false,
}: {
  children: React.ReactNode;
  topBarEnd?: React.ReactNode;
  wide?: boolean;
}): React.JSX.Element {
  return (
    <div className="gg-page page-full-height page-bg">
      <div className="gg-page__bar">
        <div className="gg-page__bar-start">
          <HomeLink />
          <ThemeToggle />
        </div>
        {topBarEnd}
      </div>
      <div className={"gg-card gg-page__card" + (wide ? " gg-card--wide" : "")}>{children}</div>
      <SiteFooter pinned />
    </div>
  );
}
