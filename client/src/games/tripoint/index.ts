import { GeoAltFill } from "react-bootstrap-icons";
import { defineGame } from "../types";
import TripointGame from "./TripointGame";

export default defineGame({
  type: "TRIPOINT",
  title: "Tripoints",
  singular: "Tripoint",
  description: "Name the three countries that meet at a point.",
  Icon: GeoAltFill,
  Component: TripointGame,
  answerNames: (tripoint) => tripoint.countries.map((country) => country.name),
});
