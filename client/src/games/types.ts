import type { ComponentType } from "react";
import type { Icon } from "react-bootstrap-icons";
import type { AnswerState, Round } from "./common/Model";

export interface GameComponentProps<T> {
  /** This round's data: the parsed jsonBlob of the server's round. */
  data: T;
  round: Round;
  answerCount: number;
  submitAnswer: (answerState: AnswerState) => void;
}

/** Everything the app needs to know about one type of game. Register it in games/registry.ts. */
export interface GameModule<T> {
  /** Must equal the backend GameType name, for example "TRIPOINT". */
  type: string;
  /** Plural name: the home page card and the browser tab title. */
  title: string;
  /** Singular name: the label of a round on the summary page. */
  singular: string;
  /** The home page card text. */
  description: string;
  Icon: Icon;
  Component: ComponentType<GameComponentProps<T>>;
  /** The answers of a round, for the summary page. */
  answerNames: (data: T) => string[];
}

export type AnyGameModule = GameModule<unknown>;

/**
 * Registers a game module's types for use with data of an unknown shape.
 *
 * The server sends each round's data as JSON, so the app only ever has `unknown` data and finds
 * the right module by game type name. Each module trusts that the server sent its own shape, so
 * this is the one place the data type is erased.
 */
export function defineGame<T>(module: GameModule<T>): AnyGameModule {
  return module as unknown as AnyGameModule;
}
