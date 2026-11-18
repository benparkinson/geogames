import { Shuffle } from "react-bootstrap-icons";
import type { Icon } from "react-bootstrap-icons";
import coastline from "./countries/coastline";
import cities from "./countries/cities";
import countryRivers from "./countries/countryRivers";
import landBorders from "./countries/landBorders";
import borderShapes from "./pairs/borderShapes";
import mergedCountries from "./pairs/mergedCountries";
import lakeShapes from "./lakes";
import countriesByNeighbours from "./countries/neighbours";
import riverCountries from "./rivers/countries";
import riverShapes from "./rivers/shapes";
import tripoint from "./tripoint";
import type { AnyGameModule } from "./types";

/** Every type of game, in home page order. To add a game, build its module and add it here. */
export const GAME_MODULES: AnyGameModule[] = [tripoint, countriesByNeighbours, landBorders, mergedCountries, cities, countryRivers, riverCountries, coastline, riverShapes, lakeShapes, borderShapes];

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
  /** Wrong guesses get a distance clue, so the Hard mode switch is relevant. */
  hasClues?: boolean;
}

/** The request-only game type that mixes every registered game. It has no module of its own. */
export const RANDOM_OPTION: MenuOption = {
  type: "RANDOM",
  title: "Mixed",
  description: "A random mix of every game.",
  Icon: Shuffle,
  hasClues: true,
};

/** The home page cards in order: the standard games, the mix, then the extra difficult games. */
export const MENU_OPTIONS: MenuOption[] = [
  ...GAME_MODULES.filter((module) => !module.extraDifficult),
  RANDOM_OPTION,
  ...GAME_MODULES.filter((module) => module.extraDifficult),
];

export const ROUND_OPTIONS = [3, 5, 10];
