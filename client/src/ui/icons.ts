// The only file that imports react-bootstrap-icons (ESLint enforces it). Icons are named by meaning, so a redesign
// swaps a glyph here and nowhere else.
import type { Icon } from "react-bootstrap-icons";

export type IconComponent = Icon;

export {
  ArrowUp as BearingIcon,
  Bezier2 as BorderShapeIcon,
  BoundingBox as LandBordersIcon,
  Buildings as CitiesIcon,
  GearFill as SettingsIcon,
  CalendarCheck as DailyIcon,
  CheckLg as FoundIcon,
  CupHotFill as SupportIcon,
  DropletFill as LakeIcon,
  EyeFill as RevealIcon,
  Fire as ExtraDifficultIcon,
  GeoAltFill as TripointIcon,
  Globe as ContinentIcon,
  Lightbulb as HintIcon,
  LightningChargeFill as StreakIcon,
  PuzzleFill as NeighboursIcon,
  Shuffle as RandomIcon,
  Tsunami as CoastlineIcon,
  Union as MergedCountriesIcon,
  Water as RiverIcon,
  X as CloseIcon,
  XLg as WrongGuessIcon,
  XLg as OutOfLivesIcon,
  ArrowRight as NextIcon,
} from "react-bootstrap-icons";
