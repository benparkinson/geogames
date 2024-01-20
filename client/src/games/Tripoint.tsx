import TripointMapWrapper from "../map/TripointMapWrapper";
import { normaliseString } from "../helper/stringHelper";
import type { BBox, GeoJsonObject, GeoJsonProperties, MultiPolygon, Polygon } from 'geojson';
import MapGame from "./common/MapGame";

function Tripoint({ tripoint, round, submitAnswer }): JSX.Element {
  function countryNames(tripoint: TripointModel): string[] {
    return tripoint.countries.map(country => country.name);
  }

  function correctAnswers(tripoint: TripointModel): string[] {
    return countryNames(tripoint).slice();
  }

  function checkAdditionalAnswers(guess: string, tripoint: TripointModel): string {
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

  function isAnswerCorrect(guess: string, tripoint: TripointModel): boolean {
    const normalisedGuess = checkAdditionalAnswers(guess, tripoint);

    return correctAnswers(tripoint)
      .map(answer => normaliseString(answer))
      .includes(normaliseString(normalisedGuess));
  }

  function renderMapGame() {
    return (
      <MapGame
        data={tripoint}
        guessBoxCount={3}
        MapComponent={TripointMapWrapper}
        correctAnswersFunction={correctAnswers}
        round={round}
        explanation={explanation}
        submitAnswer={submitAnswer}
        answerState={round.answerState}
        isAnswerCorrect={isAnswerCorrect}
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

const explanation: string = "Guess the countries that meet at this tripoint! You can zoom in and out of the map to help you.";

export default Tripoint;
