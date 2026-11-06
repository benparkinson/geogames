import dynamic from "next/dynamic";
import { useMemo } from "react";
import { Spinner } from "react-bootstrap";
import { River } from "../games/RiverShapes";
import { MapProps } from "./MapProps";

function RiverShapesMapWrapper({ data, gameOver, gaveUp }: MapProps<River>) {
  const RiverShapesMap = useMemo(
    () =>
      dynamic(() => import("./RiverShapesMap"), {
        loading: () => <div className="d-flex h-100 align-items-center justify-content-center"><Spinner animation="border" /></div>,
        ssr: false,
      }),
    []
  );
  return <RiverShapesMap river={data} gameOver={gameOver} gaveUp={gaveUp} />;
}

export default RiverShapesMapWrapper;
