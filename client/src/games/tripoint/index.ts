import { defineGame } from "../types";
import TripointGame from "./TripointGame";
import { TripointIcon } from "../../ui/icons";

export default defineGame({
  type: "TRIPOINT",
  description: "Name the three countries that meet at a point.",
  Icon: TripointIcon,
  Component: TripointGame,
  answers: (tripoint) => tripoint.countries,
});
