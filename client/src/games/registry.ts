import { Shuffle } from "react-bootstrap-icons";
import type { Icon } from "react-bootstrap-icons";
import coastline from "./countries/coastline";
import landBorders from "./countries/landBorders";
import lakeShapes from "./lakes";
import countriesByNeighbours from "./countries/neighbours";
import riverCountries from "./rivers/countries";
import riverEnds from "./rivers/ends";
import riverOnMap from "./rivers/onMap";
import riverShapes from "./rivers/shapes";
import tripoint from "./tripoint";
import type { AnyGameModule } from "./types";

/** Every type of game, in home page order. To add a game, build its module and add it here. */
export const GAME_MODULES: AnyGameModule[] = [tripoint, countriesByNeighbours, landBorders, riverOnMap, riverEnds, riverCountries, coastline, riverShapes, lakeShapes];

export function findGameModule(type: string): AnyGameModule | undefined {
  return GAME_MODULES.find((module) => module.type === type);
}

/** What the home page needs to show a card for a choice of game. */
export interface MenuOption {
  type: string;
  title: string;
  description: string;
  Icon: Icon;
  extraDifficult?: boolean;
}

/** The request-only game type that mixes every registered game. It has no module of its own. */
export const RANDOM_OPTION: MenuOption = {
  type: "RANDOM",
  title: "Mixed",
  description: "A random mix of every game.",
  Icon: Shuffle,
};

/** The home page cards in order: the standard games, the mix, then the extra difficult games. */
export const MENU_OPTIONS: MenuOption[] = [
  ...GAME_MODULES.filter((module) => !module.extraDifficult),
  RANDOM_OPTION,
  ...GAME_MODULES.filter((module) => module.extraDifficult),
];

export const ROUND_OPTIONS = [3, 5, 10];
