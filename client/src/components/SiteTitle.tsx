import Link from "next/link";

/** The site icon and name. `collapsible` hides the name on a phone, leaving the icon. */
function SiteName({ collapsible }: { collapsible: boolean }): React.JSX.Element {
  return (
    <>
      {/* eslint-disable-next-line @next/next/no-img-element -- a static export can't optimise images; this one is a small fixed PNG */}
      <img className="gg-site-title__logo" src="/logo.png" alt="" width={128} height={128} />
      <span className={collapsible ? "gg-site-title__name gg-site-title__name--collapsible" : "gg-site-title__name"}>
        GeoGauntlet
      </span>
    </>
  );
}

/** The home page's h1: the site icon and name. */
export function SiteHeading(): React.JSX.Element {
  return (
    <h1 className="gg-site-header__title gg-site-title">
      <SiteName collapsible={false} />
    </h1>
  );
}

/**
 * The site icon and name at the start of every header except the home page's (which shows `SiteHeading`): the same
 * type, as a link home. In a game's top bar, `collapsible` keeps only the icon on a phone.
 */
export default function SiteTitle({ collapsible = false }: { collapsible?: boolean }): React.JSX.Element {
  return (
    <div className="gg-site-header__title">
      <Link href="/" aria-label="Home" className="gg-text-default gg-site-header__home gg-site-title">
        <SiteName collapsible={collapsible} />
      </Link>
    </div>
  );
}
