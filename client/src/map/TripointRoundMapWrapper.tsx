import dynamic from "next/dynamic";
import { useMemo } from "react";
import { GameModel } from "../games/common/Model";
import { MapProps } from "./MapProps";

function TripointMapWrapper({ data, gameOver, gaveUp }: MapProps<GameModel>): JSX.Element {
  const TripointMap = useMemo(
    () =>
      dynamic(() => import("./TripointMap"), {
        loading: () => <p>A map is loading</p>,
        ssr: false
      }),
    []
  );

  const tripoint = JSON.parse(data.rounds[0].jsonBlob)

  return <TripointMap tripoint={tripoint} gameOver={gameOver} gaveUp={gaveUp} />;
}

export default TripointMapWrapper;
