import RiverShapesMapWrapper from "../map/RiverShapesMapWrapper";
import type { BBox, GeoJsonObject, GeoJsonProperties, MultiLineString } from 'geojson';
import MapGame from "./common/MapGame";

function RiverShapes() {
  function guessBoxName() {
    return "River name";
  }

  function correctAnswers(river) {
    return [river.properties.name];
  }

  function checkAdditionalAnswers(guess, river) {
    return guess;
  }

  return (
    <MapGame
      dataName={"River"}
      serverRoute={"/api/river-shape"}
      guessBoxCount={1}
      guessBoxName={guessBoxName}
      MapComponent={RiverShapesMapWrapper}
      correctAnswersFunction={correctAnswers}
      checkAdditionalAnswers={checkAdditionalAnswers}
    />
  );
}

export class River implements GeoJsonObject {
  type: "MultiLineString";
  bbox?: BBox;
  properties: GeoJsonProperties
  geometry: MultiLineString
}

export default RiverShapes;
