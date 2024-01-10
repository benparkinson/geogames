import TripointMapWrapper from "../map/TripointMapWrapper";
import { normaliseString } from "../helper/stringHelper";
import type { BBox, GeoJsonObject, GeoJsonProperties, MultiPolygon, Polygon } from 'geojson';
import MapGame from "./common/MapGame";

function Tripoint({ tripoint, round }): JSX.Element {
  function guessBoxName(index: number): string {
    return "Country " + (index + 1);
  }

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

  function renderMapGame() {
    return (
      <MapGame
        data={tripoint}
        guessBoxCount={3}
        guessBoxName={guessBoxName}
        MapComponent={TripointMapWrapper}
        correctAnswersFunction={correctAnswers}
        checkAdditionalAnswers={checkAdditionalAnswers}
        round={round}
        explanation={explanation}
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
