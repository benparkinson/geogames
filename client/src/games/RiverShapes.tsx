import RiverShapesMapWrapper from "../map/RiverShapesMapWrapper";
import { normaliseString } from "../helper/stringHelper";
import type { BBox, GeoJsonObject, GeoJsonProperties, MultiLineString } from 'geojson';
import MapGame from "./common/MapGame";

function RiverShapes({ river, round, submitAnswer }): JSX.Element {

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

  function isAnswerCorrect(guess: string, riverModel: River): boolean {
    const normalisedGuess = checkAdditionalAnswers(guess, riverModel);

    return normaliseString(normalisedGuess) === normaliseString(riverModel.name);
  }

  return (
    <MapGame
      data={river}
      guessBoxCount={1}
      MapComponent={RiverShapesMapWrapper}
      correctAnswersFunction={correctAnswers}
      round={round}
      explanation={explanation}
      clues={["Continent: " + river.continent]}
      submitAnswer={submitAnswer}
      answerState={round.answerState}
      isAnswerCorrect={isAnswerCorrect}
    />
  );
}

export class River {
  name: string;
  additionalNames: string[];
  geoData: RiverGeoData;
  continent: string;
}

export class RiverGeoData implements GeoJsonObject {
  type: "MultiLineString";
  bbox?: BBox;
  properties: GeoJsonProperties;
  geometry: MultiLineString;
}

const explanation: string = "Guess the name of the river from its shape! You can zoom in and out of the map to help you.";

export default RiverShapes;
