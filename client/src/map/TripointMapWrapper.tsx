import dynamic from "next/dynamic";
import { useMemo } from "react";
import { TripointModel } from "../games/TripointRound";
import { MapProps } from "./MapProps";

function TripointMapWrapper({ data, gameOver, gaveUp }: MapProps<TripointModel>): JSX.Element {
  const TripointMap = useMemo(
    () =>
      dynamic(() => import("./TripointMap"), {
        loading: () => <p>A map is loading</p>,
        ssr: false
      }),
    []
  );
  return <TripointMap tripoint={data} gameOver={gameOver} gaveUp={gaveUp} />;
}

export default TripointMapWrapper;
