import dynamic from "next/dynamic";
import { useMemo } from "react";

function RiverShapesMapWrapper({ river, gameOver, gaveUp }) {
  const RiverShapesMap = useMemo(
    () =>
      dynamic(() => import("./RiverShapesMap"), {
        loading: () => <p>A map is loading</p>,
        ssr: false,
      }),
    []
  );
  return <RiverShapesMap river={river} gameOver={gameOver} gaveUp={gaveUp} />;
}

export default RiverShapesMapWrapper;
