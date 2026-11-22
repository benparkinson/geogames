import HomeLink from "./HomeLink";
import SiteFooter from "./SiteFooter";
import SiteHeader from "./SiteHeader";

/**
 * The frame of a page with one card on it: the site header (home link, streak and theme switch), the card and the
 * footer. `topBarEnd` goes in the header before the streak; `wide` makes the card wider; `bare` drops the card's
 * own border and padding, for content (the daily ticket) that draws its own.
 */
export default function PageShell({
  children,
  topBarEnd,
  wide = false,
  bare = false,
}: {
  children: React.ReactNode;
  topBarEnd?: React.ReactNode;
  wide?: boolean;
  bare?: boolean;
}): React.JSX.Element {
  return (
    <div className="gg-page page-full-height page-bg">
      <SiteHeader title={<HomeLink />} extra={topBarEnd} />
      <div className={"gg-card gg-page__card" + (wide ? " gg-card--wide" : "") + (bare ? " gg-page__card--bare" : "")}>
        {children}
      </div>
      <SiteFooter pinned />
    </div>
  );
}
