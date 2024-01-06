import TripointMapWrapper from "../map/TripointRoundMapWrapper";
import { normaliseString } from "../helper/stringHelper";
import type { BBox, GeoJsonObject, GeoJsonProperties, MultiPolygon, Polygon } from 'geojson';
import MapGame from "./common/MapGame";
import { GameRoundModel } from "./common/Model";

function Tripoint({ gameId, round, nextRound, prevRound }): JSX.Element {
  function getTripoint(gameRound: GameRoundModel): TripointModel {
    console.log(gameRound);
    return JSON.parse(gameRound.jsonBlob);
  }

  function guessBoxName(index: number): string {
    return "Country " + (index + 1);
  }

  function countryNames(tripoint: TripointModel): string[] {
    return tripoint.countries.map(country => country.name);
  }

  function correctAnswers(gameRound: GameRoundModel): string[] {
    const tripoint = getTripoint(gameRound);
    return countryNames(tripoint).slice();
  }

  function checkAdditionalAnswers(guess: string, gameRound: GameRoundModel): string {
    const tripoint = getTripoint(gameRound);
    let normalisedGuess: string = guess;

    tripoint.countries.forEach((country: TripointCountry) =>
      country.additionalNames.forEach(additionalName => {
        if (normaliseString(guess) === normaliseString(additionalName)) {
          normalisedGuess = country.name;
        }
      })
    );
    return normalisedGuess;
  }

  function renderMapGame() {
    if (!gameId) {
      return <></>;
    }
    return (
      <MapGame
        dataName={"Tripoint"}
        serverRoute={"/api/games/" + gameId + "/rounds/" + round}
        guessBoxCount={3}
        guessBoxName={guessBoxName}
        MapComponent={TripointMapWrapper}
        correctAnswersFunction={correctAnswers}
        checkAdditionalAnswers={checkAdditionalAnswers}
        roundFunctions={{ nextRound: nextRound, prevRound: prevRound, hasPreviousRound: round > 0, hasNextRound: round < 4 }}
      />
    );
  }

  return renderMapGame();
}

export class TripointModel {
  coordinate: TripointCoordinate;
  countries: TripointCountry[];
}

export class TripointCoordinate {
  latitude: number;
  longitude: number;
}

export class TripointCountry {
  name: string;
  additionalNames: string[];
  geoData: TripointGeoData;
}

export class TripointGeoData implements GeoJsonObject {
  type: "MultiPolygon" | "Polygon";
  bbox?: BBox;
  properties: GeoJsonProperties;
  geometry: MultiPolygon | Polygon;
}

export default Tripoint;
