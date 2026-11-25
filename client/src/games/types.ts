import type { AnswerCandidate } from "./common/answers";
import type { ComponentType } from "react";
import type { Round, RoundAnswer } from "./common/Model";
import type { IconComponent } from "../ui/icons";
import { GAME_NAMES, type GameTypeName } from "./names";

export interface GameComponentProps<T> {
  /** This round's data: the parsed jsonBlob of the server's round. */
  data: T;
  round: Round;
  answerCount: number;
  submitAnswer: (answer: RoundAnswer) => void;
}

/** Everything the app needs to know about one type of game. Register it in games/registry.ts. */
export interface GameModule<T> {
  /**
   * Must equal the backend GameType name, for example "TRIPOINT".
   *
   * The number of hints comes from the backend GameType.hintCount (`round.hintCount`). The
   * component supplies the hints' content and must offer exactly that many.
   */
  type: Exclude<GameTypeName, "RANDOM">;
  /** The game's name, `GAME_NAMES[type]`: set by `defineGame`, never written in a module. */
  title: string;
  /** The home page card text. */
  description: string;
  Icon: IconComponent;
  /** Shown in the home page's "Extra difficult" section, after the other games. */
  extraDifficult?: boolean;
  /** Wrong guesses get a distance and direction clue, so Hard mode matters (see `clueFor`). */
  hasClues?: boolean;
  Component: ComponentType<GameComponentProps<T>>;
  /** The answers of a round with their other names, for the summary page (which ones were found). */
  answers: (data: T) => AnswerCandidate[];
}

export type AnyGameModule = GameModule<unknown>;

/**
 * Registers a game module's types for use with data of an unknown shape.
 *
 * The server sends each round's data as JSON, so the app only ever has `unknown` data and finds
 * the right module by game type name. Each module trusts that the server sent its own shape, so
 * this is the one place the data type is erased.
 */
export function defineGame<T>(module: Omit<GameModule<T>, "title">): AnyGameModule {
  return { ...module, title: GAME_NAMES[module.type] } as unknown as AnyGameModule;
}
