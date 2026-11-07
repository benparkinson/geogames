import Link from "next/link";
import { HouseFill } from "react-bootstrap-icons";

export default function HomeLink({ hideTextOnMobile = false }: { hideTextOnMobile?: boolean }): React.JSX.Element {
  return (
    <Link href="/" aria-label="Home" className="text-decoration-none text-body fw-semibold text-nowrap">
      <HouseFill aria-hidden="true" /> <span className={hideTextOnMobile ? "d-none d-sm-inline" : undefined}>Geogames</span>
    </Link>
  );
}
