import TripointMapWrapper from "../map/TripointMapWrapper";
import { normaliseString } from "../helper/stringHelper";
import type { BBox, GeoJsonObject, GeoJsonProperties, MultiPolygon, Polygon } from 'geojson';
import MapGame from "./common/MapGame";

function Tripoint(): JSX.Element {
  function guessBoxName(index: number): string {
    return "Country " + (index + 1);
  }

  function countryNames(tripoint: TripointModel): string[] {
    return tripoint.countries.map(country => country.properties.name);
  }

  function extractCountries(tripoint: TripointModel): GeoJsonProperties[] {
    return tripoint.countries.map(country => country.properties);
  }

  function correctAnswers(tripoint: TripointModel): string[] {
    return countryNames(tripoint).slice();
  }

  function checkAdditionalAnswers(guess: string, tripoint: TripointModel): string {
    const countries = extractCountries(tripoint);

    let normalisedGuess = guess;

    countries.forEach(country =>
      country.additionalNames.forEach(additionalName => {
        if (normaliseString(guess) === normaliseString(additionalName)) {
          normalisedGuess = country.name;
        }
      })
    );
    return normalisedGuess;
  }

  return (
    <MapGame
      dataName={"Tripoint"}
      serverRoute={"/api/tripoint"}
      guessBoxCount={3}
      guessBoxName={guessBoxName}
      MapComponent={TripointMapWrapper}
      correctAnswersFunction={correctAnswers}
      checkAdditionalAnswers={checkAdditionalAnswers}
    />
  );
}

export class TripointModel {
  coordinate: TripointCoordinate;
  countries: TripointCountry[];
}

export class TripointCoordinate {
  latitude: number;
  longitude: number;
}

export class TripointCountry implements GeoJsonObject {
  type: "MultiPolygon" | "Polygon";
  bbox?: BBox;
  properties: GeoJsonProperties
  geometry: MultiPolygon | Polygon
}

export default Tripoint;
