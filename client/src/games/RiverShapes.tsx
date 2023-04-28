import RiverShapesMapWrapper from "../map/RiverShapesMapWrapper";
import { normaliseString } from "../helper/stringHelper";
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
    let normalisedGuess: string = guess;

    river.additionalNames.forEach(additionalName => {
      if (normaliseString(guess) === normaliseString(additionalName)) {
        normalisedGuess = river.name;
      }
    });

    return normalisedGuess;
  }

  function queryParams(river: River): string {
    return "";
  }

  return (
    <MapGame
      dataName={"River"}
      serverRoute={"/api/river-shape"}
      queryParamsFromPreviousResponse={queryParams}
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
  additionalNames: string[];
  geoData: RiverGeoData;
}

export class RiverGeoData implements GeoJsonObject {
  type: "MultiLineString";
  bbox?: BBox;
  properties: GeoJsonProperties;
  geometry: MultiLineString;
}

export default RiverShapes;
