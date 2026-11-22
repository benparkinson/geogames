import Link from "next/link";
import { HomeIcon } from "../ui/icons";

export default function HomeLink({ hideTextOnMobile = false }: { hideTextOnMobile?: boolean }): React.JSX.Element {
  return (
    <Link href="/" aria-label="Home" className="gg-home-link gg-text-default">
      <HomeIcon aria-hidden="true" />{" "}
      <span className={hideTextOnMobile ? "gg-home-link__name--collapsible" : undefined}> GeoGauntlet</span>
    </Link>
  );
}
