import TripointMapWrapper from "../map/TripointMapWrapper";
import { normaliseString } from "../helper/stringHelper";
import type { BBox, GeoJsonObject, GeoJsonProperties, MultiPolygon, Polygon } from 'geojson';
import MapGame from "./common/MapGame";
import { useMemo, useState } from "react";
import { useCountryNames } from "../api/countries";

function Tripoint({ tripoint, round, answerCount, submitAnswer }): React.JSX.Element {
  const [data, setData] = useState(tripoint);
  const { data: countryList } = useCountryNames();

  const suggestions = useMemo(() => countryList?.map(c => c.name), [countryList]);
  const knownNames = useMemo(() => {
    if (!countryList) {
      return null;
    }
    const names = new Set<string>();
    countryList.forEach(c => {
      names.add(normaliseString(c.name));
      c.additionalNames.forEach(alias => names.add(normaliseString(alias)));
    });
    return names;
  }, [countryList]);

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

  function showRandomCountryBorder(): void {
    const remaining = data.countries
      .filter(country => !country.showAsHint)

    const alreadyShown = data.countries.filter(country => country.showAsHint);

    if (remaining.length === 0) {
      return;
    }

    // Only called from an event handler, never during render.
    // eslint-disable-next-line react-hooks/purity
    const randomCountry = remaining[Math.floor(Math.random() * remaining.length)];

    randomCountry.showAsHint = true;

    const newCountries = [...alreadyShown, randomCountry];
    newCountries.push(...remaining.filter(country => country !== randomCountry));

    const newData = {
      ...data,
      countries: newCountries
    }

    setData(newData);
  }

  function renderMapGame() {

    return (
      <MapGame
        data={data}
        answerCount={answerCount}
        MapComponent={TripointMapWrapper}
        correctAnswersFunction={correctAnswers}
        round={round}
        suggestions={suggestions}
        isKnownGuess={knownNames ? (guess: string) => knownNames.has(normaliseString(guess)) : undefined}
        prompt={`Name the ${answerCount} countries that meet at the flag.`}
        inputPlaceholder="Country name…"
        hints={{
          total: data.countries.length,
          description: "Shows the outline of one of the countries",
          use: () => {
            showRandomCountryBorder();
            return null;
          }
        }}
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
  showAsHint: boolean = false;
}

export class TripointGeoData implements GeoJsonObject {
  type: "MultiPolygon" | "Polygon";
  bbox?: BBox;
  properties: GeoJsonProperties;
  geometry: MultiPolygon | Polygon;
}

export default Tripoint;
