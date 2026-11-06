import RiverShapesMapWrapper from "../map/RiverShapesMapWrapper";
import { normaliseString } from "../helper/stringHelper";
import type { BBox, GeoJsonObject, GeoJsonProperties, MultiLineString } from 'geojson';
import MapGame from "./common/MapGame";

function RiverShapes({ river, round, answerCount, submitAnswer }): React.JSX.Element {

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
      answerCount={answerCount}
      MapComponent={RiverShapesMapWrapper}
      correctAnswersFunction={correctAnswers}
      round={round}
      blankMapCaption="Shape only. The satellite map appears when you answer."
      prompt="Name this river from its shape."
      inputPlaceholder="River name…"
      hints={{
        total: 1,
        description: "Shows which continent the river is in",
        use: () => `Continent: ${river.continent}`
      }}
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

export default RiverShapes;
