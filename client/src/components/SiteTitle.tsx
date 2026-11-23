import Link from "next/link";

/** The site name in the header of every page except the home page (which shows it as its h1): the same type, as a link home. */
export default function SiteTitle(): React.JSX.Element {
  return (
    <div className="gg-site-header__title">
      <Link href="/" aria-label="Home" className="gg-text-default gg-site-header__home">
        GeoGauntlet
      </Link>
    </div>
  );
}
