import dynamic from "next/dynamic";
import { useMemo } from "react";
import { GameRoundModel } from "../games/common/Model";
import { MapProps } from "./MapProps";

function TripointMapWrapper({ data, gameOver, gaveUp }: MapProps<GameRoundModel>): JSX.Element {
  const TripointMap = useMemo(
    () =>
      dynamic(() => import("./TripointMap"), {
        loading: () => <p>A map is loading</p>,
        ssr: false
      }),
    []
  );

  const tripoint = JSON.parse(data.jsonBlob)

  return <TripointMap tripoint={tripoint} gameOver={gameOver} gaveUp={gaveUp} />;
}

export default TripointMapWrapper;
