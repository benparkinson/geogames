import dynamic from "next/dynamic";
import { useMemo } from "react";

function TripointMapWrapper({ tripoint, gameOver, gaveUp }) {
  const Map = useMemo(
    () =>
      dynamic(() => import("../map/Map"), {
        loading: () => <p>A map is loading</p>,
        ssr: false,
      }),
    []
  );
  return <Map tripoint={tripoint} gameOver={gameOver} gaveUp={gaveUp} />;
}

export default TripointMapWrapper;
