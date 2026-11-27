import Link from "next/link";
import { LANDING_TYPES, landingPath, type LandingType } from "../games/landing";
import { GAME_NAMES } from "../games/names";

/** A link to every game's landing page, but for `except`. */
export default function GameLinks({
  className,
  except,
}: {
  className: string;
  except?: LandingType;
}): React.JSX.Element {
  return (
    <ul className={className}>
      {LANDING_TYPES.filter((type) => type !== except).map((type) => (
        <li key={type}>
          {/* No prefetch: a footer on every page would fetch every landing page. */}
          <Link href={landingPath(type)} prefetch={false}>
            {GAME_NAMES[type]}
          </Link>
        </li>
      ))}
    </ul>
  );
}
