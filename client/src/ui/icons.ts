// The only file that imports react-bootstrap-icons (ESLint enforces it). Icons are named by meaning, so a redesign
// swaps a glyph here and nowhere else.
import { CheckLg, EyeFill, StarFill, XLg } from "react-bootstrap-icons";
import type { Icon } from "react-bootstrap-icons";

export type IconComponent = Icon;

export {
  ArrowUp as BearingIcon,
  Bezier2 as BorderShapeIcon,
  BoundingBox as LandBordersIcon,
  Buildings as CitiesIcon,
  CalendarCheck as DailyIcon,
  CheckLg as FoundIcon,
  CupHotFill as SupportIcon,
  DropletFill as LakeIcon,
  EyeFill as RevealIcon,
  Fire as ExtraDifficultIcon,
  GeoAltFill as TripointIcon,
  Globe as ContinentIcon,
  Heart as LifeLostIcon,
  HeartFill as LifeIcon,
  HouseFill as HomeIcon,
  Lightbulb as HintIcon,
  LightningChargeFill as StreakIcon,
  PuzzleFill as NeighboursIcon,
  Shuffle as RandomIcon,
  Tsunami as CoastlineIcon,
  Union as MergedCountriesIcon,
  Water as RiverIcon,
  X as CloseIcon,
  XLg as WrongGuessIcon,
  ArrowRight as NextIcon,
} from "react-bootstrap-icons";

/** One glyph per round result, shared by the progress dots, the round banner and the results rows. */
export const RESULT_ICONS: Record<"firstGuess" | "correct" | "hints" | "revealed" | "outOfGuesses", IconComponent> = {
  firstGuess: StarFill,
  correct: CheckLg,
  hints: CheckLg,
  revealed: EyeFill,
  outOfGuesses: XLg,
};
