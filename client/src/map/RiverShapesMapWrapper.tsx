import dynamic from "next/dynamic";
import { useMemo } from "react";
import { River } from "../games/RiverShapes";
import { MapProps } from "./MapProps";

function RiverShapesMapWrapper({ data, gameOver, gaveUp }: MapProps<River>) {
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
