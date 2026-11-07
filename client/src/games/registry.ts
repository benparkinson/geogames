import { Shuffle } from "react-bootstrap-icons";
import type { Icon } from "react-bootstrap-icons";
import riverCountries from "./rivers/countries";
import riverEnds from "./rivers/ends";
import riverOnMap from "./rivers/onMap";
import riverShapes from "./rivers/shapes";
import tripoint from "./tripoint";
import type { AnyGameModule } from "./types";

/** Every type of game. To add a game, build its module and add it here. */
export const GAME_MODULES: AnyGameModule[] = [tripoint, riverShapes, riverOnMap, riverEnds, riverCountries];

export function findGameModule(type: string): AnyGameModule | undefined {
  return GAME_MODULES.find((module) => module.type === type);
}

/** What the home page needs to show a card for a choice of game. */
export interface MenuOption {
  type: string;
  title: string;
  description: string;
  Icon: Icon;
}

/** The request-only game type that mixes every registered game. It has no module of its own. */
export const RANDOM_OPTION: MenuOption = {
  type: "RANDOM",
  title: "Mixed",
  description: "A random mix of every game.",
  Icon: Shuffle,
};

export const MENU_OPTIONS: MenuOption[] = [...GAME_MODULES, RANDOM_OPTION];

export const ROUND_OPTIONS = [3, 5, 10];
