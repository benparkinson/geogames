import Link from "next/link";
import { HomeIcon } from "../ui/icons";

export default function HomeLink({ hideTextOnMobile = false }: { hideTextOnMobile?: boolean }): React.JSX.Element {
  return (
    <Link href="/" aria-label="Home" className="text-decoration-none gg-text-default fw-semibold text-nowrap">
      <HomeIcon aria-hidden="true" />{" "}
      <span className={hideTextOnMobile ? "d-none d-sm-inline" : undefined}> GeoGauntlet</span>
    </Link>
  );
}
