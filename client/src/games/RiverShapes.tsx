import RiverShapesMapWrapper from "../map/RiverShapesMapWrapper";
import type { BBox, GeoJsonObject, GeoJsonProperties, MultiLineString } from 'geojson';
import MapGame from "./common/MapGame";

function RiverShapes(): JSX.Element {
  function guessBoxName(): string {
    return "River name";
  }

  function correctAnswers(river: River): string[] {
    return [river.name];
  }

  function checkAdditionalAnswers(guess: string, river: River): string {
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

export class River {
  name: string;
  geoData: RiverGeoData;
}

export class RiverGeoData implements GeoJsonObject {
  type: "MultiLineString";
  bbox?: BBox;
  properties: GeoJsonProperties;
  geometry: MultiLineString;
}

export default RiverShapes;
