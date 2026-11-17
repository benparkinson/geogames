import { CircleMarker, Tooltip } from "react-leaflet";
import { capitalDotOptions, cityDotOptions, cityRadius } from "../../map/constants";
import { FitBounds } from "../../map/FitBounds";
import type { City, CountryShape } from "./model";

/** The biggest capital dots are no bigger than a city of their size; the smallest are still easy to see. */
const CAPITAL_MIN_RADIUS = 6;

function largestPopulation(cities: City[]): number {
  return Math.max(0, ...cities.map((city) => city.population));
}

/** A dot for every city, sized by population. The names show on hover once the round is over. */
export function CityDots({ cities, labelled = false }: { cities: City[]; labelled?: boolean }): React.JSX.Element {
  const largest = largestPopulation(cities);
  return (
    <>
      {cities.map((city) => (
        <CircleMarker
          key={`${city.name}/${city.lon}/${city.lat}`}
          center={[city.lat, city.lon]}
          radius={cityRadius(city.population, largest)}
          pathOptions={cityDotOptions()}
        >
          {labelled && <Tooltip sticky>{city.name}</Tooltip>}
        </CircleMarker>
      ))}
    </>
  );
}

/** Draws the capital again over its dot, in a clearly different style. */
export function CapitalDots({ cities, labelled = false }: { cities: City[]; labelled?: boolean }): React.JSX.Element {
  const largest = largestPopulation(cities);
  return (
    <>
      {cities
        .filter((city) => city.capital)
        .map((city) => (
          <CircleMarker
            key={`${city.name}/${city.lon}/${city.lat}/capital`}
            center={[city.lat, city.lon]}
            radius={Math.max(cityRadius(city.population, largest), CAPITAL_MIN_RADIUS)}
            pathOptions={capitalDotOptions()}
          >
            {labelled && <Tooltip sticky>{city.name}</Tooltip>}
          </CircleMarker>
        ))}
    </>
  );
}

/** Fits the map to the country's main part and every city, so a far-off city is in view from the start. */
export function CitiesFit({ country, cities }: { country: CountryShape; cities: City[] }): React.JSX.Element {
  const { south, west, north, east } = country.focus;
  return (
    <FitBounds
      id={country.name}
      bounds={[
        [Math.min(south, ...cities.map((city) => city.lat)), Math.min(west, ...cities.map((city) => city.lon))],
        [Math.max(north, ...cities.map((city) => city.lat)), Math.max(east, ...cities.map((city) => city.lon))],
      ]}
    />
  );
}
