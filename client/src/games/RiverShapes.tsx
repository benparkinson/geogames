import RiverShapesMapWrapper from "../map/RiverShapesMapWrapper";
import { normaliseString } from "../helper/stringHelper";
import type { BBox, GeoJsonObject, GeoJsonProperties, MultiLineString } from 'geojson';
import MapGame from "./common/MapGame";

function RiverShapes({ river, round }): JSX.Element {
  function guessBoxName(): string {
    return "River name";
  }

  function correctAnswers(riverModel: River): string[] {
    return [riverModel.name];
  }

  function checkAdditionalAnswers(guess: string, riverModel: River): string {
    let normalisedGuess: string = guess;

    riverModel.additionalNames.forEach(additionalName => {
      if (normaliseString(guess) === normaliseString(additionalName)) {
        normalisedGuess = riverModel.name;
      }
    });

    return normalisedGuess;
  }
  return (
    <MapGame
      data={river}
      guessBoxCount={1}
      guessBoxName={guessBoxName}
      MapComponent={RiverShapesMapWrapper}
      correctAnswersFunction={correctAnswers}
      checkAdditionalAnswers={checkAdditionalAnswers}
      round={round}
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
