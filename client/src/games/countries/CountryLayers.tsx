import { GeoJSON } from "react-leaflet";
import { FitBounds } from "../../map/FitBounds";
import { answerPathOptions, clueLineOptions, clueOutlineOptions, hintLineOptions } from "../../map/constants";
import type { LineGeoData } from "../common/country";
import type { CountryOutline, CountryShape } from "./model";

/** Fits the map to the country's largest part, so overseas parts and the antimeridian don't zoom out to the world. */
export function FocusFit({ country }: { country: CountryShape }): React.JSX.Element {
  const { south, west, north, east } = country.focus;
  return (
    <FitBounds
      id={country.name}
      bounds={[
        [south, west],
        [north, east],
      ]}
    />
  );
}

/** The filled outline of each neighbour. */
export function NeighbourOutlines({ neighbours, id }: { neighbours: CountryOutline[]; id: string }): React.JSX.Element {
  return (
    <>
      {neighbours.map((neighbour) => (
        <GeoJSON key={`${id}/${neighbour.name}`} data={neighbour.outline} pathOptions={clueOutlineOptions()} />
      ))}
    </>
  );
}

/** Lines that are the clue while playing. */
export function ClueLines({ data, id }: { data: LineGeoData; id: string }): React.JSX.Element {
  return <GeoJSON key={id} data={data} pathOptions={clueLineOptions()} />;
}

/** Lines a hint adds: dashed and fainter. */
export function HintLines({ data, id }: { data: LineGeoData; id: string }): React.JSX.Element {
  return <GeoJSON key={id} data={data} pathOptions={hintLineOptions()} />;
}

/** The country's whole outline. Re-created when the round is lost or given up so the colour changes. */
export function AnswerOutline({ country, gaveUp }: { country: CountryShape; gaveUp: boolean }): React.JSX.Element {
  return (
    <GeoJSON
      pathOptions={answerPathOptions(gaveUp)}
      data={country.outline}
      key={`${country.name}/outline${gaveUp ? "/gaveUp" : ""}`}
    />
  );
}
