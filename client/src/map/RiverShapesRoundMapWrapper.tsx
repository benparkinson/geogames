import dynamic from "next/dynamic";
import { useMemo } from "react";
import { MapProps } from "./MapProps";
import { GameRoundModel } from "../games/common/Model";

function RiverShapesMapWrapper({ data, gameOver, gaveUp }: MapProps<GameRoundModel>) {
  const RiverShapesMap = useMemo(
    () =>
      dynamic(() => import("./RiverShapesMap"), {
        loading: () => <p>A map is loading</p>,
        ssr: false,
      }),
    []
  );

  const river = JSON.parse(data.jsonBlob)

  return <RiverShapesMap river={river} gameOver={gameOver} gaveUp={gaveUp} />;
}

export default RiverShapesMapWrapper;
