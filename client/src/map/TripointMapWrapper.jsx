import dynamic from "next/dynamic";
import { useMemo } from "react";

function TripointMapWrapper({ tripoint, gameOver, gaveUp }) {
  const TripointMap = useMemo(
    () =>
      dynamic(() => import("./TripointMap"), {
        loading: () => <p>A map is loading</p>,
        ssr: false,
      }),
    []
  );
  return <TripointMap tripoint={tripoint} gameOver={gameOver} gaveUp={gaveUp} />;
}

export default TripointMapWrapper;
