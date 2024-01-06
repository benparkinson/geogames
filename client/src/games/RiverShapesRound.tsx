import RiverShapesMapWrapper from "../map/RiverShapesMapWrapper";
import { normaliseString } from "../helper/stringHelper";
import type { BBox, GeoJsonObject, GeoJsonProperties, MultiLineString } from 'geojson';
import MapGame from "./common/MapGame";
import { GameRoundModel } from "./common/Model";

function RiverShapes({ gameId, round, nextRound, prevRound }): JSX.Element {
  function getRiver(gameRound: GameRoundModel): River {
    return JSON.parse(gameRound.jsonBlob);
  }

  function guessBoxName(): string {
    return "River name";
  }

  function correctAnswers(game: GameRoundModel): string[] {
    const river = getRiver(game);
    return [river.name];
  }

  function checkAdditionalAnswers(guess: string, game: GameRoundModel): string {
    const river = getRiver(game);
    let normalisedGuess: string = guess;

    river.additionalNames.forEach(additionalName => {
      if (normaliseString(guess) === normaliseString(additionalName)) {
        normalisedGuess = river.name;
      }
    });

    return normalisedGuess;
  }

  if (!gameId) {
    return <></>;
  }
  return (
    <MapGame
      dataName={"River"}
      serverRoute={"/api/games/" + gameId + "/rounds/" + round}
      guessBoxCount={1}
      guessBoxName={guessBoxName}
      MapComponent={RiverShapesMapWrapper}
      correctAnswersFunction={correctAnswers}
      checkAdditionalAnswers={checkAdditionalAnswers}
      roundFunctions={{ nextRound: nextRound, prevRound: prevRound, hasPreviousRound: round > 0, hasNextRound: round < 4 }}
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
