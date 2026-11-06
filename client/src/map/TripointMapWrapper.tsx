import dynamic from "next/dynamic";
import { useMemo } from "react";
import { Spinner } from "react-bootstrap";
import { TripointModel } from "../games/Tripoint";
import { MapProps } from "./MapProps";

function TripointMapWrapper({ data, gameOver, gaveUp }: MapProps<TripointModel>): JSX.Element {
  const TripointMap = useMemo(
    () =>
      dynamic(() => import("./TripointMap"), {
        loading: () => <div className="d-flex h-100 align-items-center justify-content-center"><Spinner animation="border" /></div>,
        ssr: false
      }),
    []
  );
  return <TripointMap tripoint={data} gameOver={gameOver} gaveUp={gaveUp} />;
}

export default TripointMapWrapper;
