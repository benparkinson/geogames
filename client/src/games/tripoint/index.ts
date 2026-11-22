import { defineGame } from "../types";
import TripointGame from "./TripointGame";
import { TripointIcon } from "../../ui/icons";

export default defineGame({
  type: "TRIPOINT",
  title: "Tripoints",
  singular: "Tripoint",
  description: "Name the three countries that meet at a point.",
  Icon: TripointIcon,
  Component: TripointGame,
  answerNames: (tripoint) => tripoint.countries.map((country) => country.name),
});
