import { FlagFill, Icon, Shuffle, Water } from "react-bootstrap-icons";

export interface GameOption {
  name: "TRIPOINT" | "RIVERS_BY_SHAPE" | "RANDOM";
  title: string;
  singular: string;
  description: string;
  Icon: Icon;
}

export const GAME_OPTIONS: GameOption[] = [
  { name: "TRIPOINT", title: "Tripoints", singular: "Tripoint", description: "Name the three countries that meet at a point.", Icon: FlagFill },
  { name: "RIVERS_BY_SHAPE", title: "Rivers", singular: "River", description: "Name a river from its shape alone.", Icon: Water },
  { name: "RANDOM", title: "Mixed", singular: "Mixed", description: "A random mix of both.", Icon: Shuffle },
];

export const ROUND_OPTIONS = [3, 5, 10];
