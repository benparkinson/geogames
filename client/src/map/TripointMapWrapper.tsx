import dynamic from "next/dynamic";
import { Spinner } from "react-bootstrap";
import { TripointModel } from "../games/Tripoint";
import { MapProps } from "./MapProps";

const TripointMap = dynamic(() => import("./TripointMap"), {
  loading: () => <div className="d-flex h-100 align-items-center justify-content-center"><Spinner animation="border" /></div>,
  ssr: false
});

function TripointMapWrapper({ data, gameOver, gaveUp }: MapProps<TripointModel>): React.JSX.Element {
  return <TripointMap tripoint={data} gameOver={gameOver} gaveUp={gaveUp} />;
}

export default TripointMapWrapper;
