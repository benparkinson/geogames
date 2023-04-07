import dynamic from "next/dynamic";
import { useMemo } from "react";

function RiverShapesMapWrapper({ data, gameOver, gaveUp }) {
  const RiverShapesMap = useMemo(
    () =>
      dynamic(() => import("./RiverShapesMap"), {
        loading: () => <p>A map is loading</p>,
        ssr: false,
      }),
    []
  );
  return <RiverShapesMap river={data} gameOver={gameOver} gaveUp={gaveUp} />;
}

export default RiverShapesMapWrapper;
