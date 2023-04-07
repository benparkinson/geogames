import dynamic from "next/dynamic";
import { useMemo } from "react";

function TripointMapWrapper({ data, gameOver, gaveUp }) {
  const TripointMap = useMemo(
    () =>
      dynamic(() => import("./TripointMap"), {
        loading: () => <p>A map is loading</p>,
        ssr: false,
      }),
    []
  );
  return <TripointMap tripoint={data} gameOver={gameOver} gaveUp={gaveUp} />;
}

export default TripointMapWrapper;
