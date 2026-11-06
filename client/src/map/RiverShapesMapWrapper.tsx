import dynamic from "next/dynamic";
import { Spinner } from "react-bootstrap";
import { River } from "../games/RiverShapes";
import { MapProps } from "./MapProps";

const RiverShapesMap = dynamic(() => import("./RiverShapesMap"), {
  loading: () => <div className="d-flex h-100 align-items-center justify-content-center"><Spinner animation="border" /></div>,
  ssr: false,
});

function RiverShapesMapWrapper({ data, gameOver, gaveUp }: MapProps<River>) {
  return <RiverShapesMap river={data} gameOver={gameOver} gaveUp={gaveUp} />;
}

export default RiverShapesMapWrapper;
