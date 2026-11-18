import { GeoJSON } from "react-leaflet";
import { FitBounds } from "../../map/FitBounds";
import { boundsToLatLng } from "../../map/boundsToLatLng";
import { hiddenBorderOptions, mergedShapeOptions } from "../../map/constants";
import { AnswerOutline } from "../countries/CountryLayers";
import type { Bounds } from "../countries/model";
import type { LineGeoData } from "../common/country";
import type { CountryPairRound } from "./model";

/** Identifies the pair, for keys and for deciding when to fit the map again. */
export function pairId(round: CountryPairRound): string {
  return round.countries.map((country) => country.name).join("|");
}

/** Fits the map to `bounds`, and again whenever `id` changes. */
export function BoundsFit({ bounds, id }: { bounds: Bounds; id: string }): React.JSX.Element {
  return (
    <FitBounds id={id} bounds={boundsToLatLng(bounds)} />
  );
}

/** Each country's whole outline, with its name on hover. Re-created when the round is given up so the colour changes. */
export function AnswerOutlines({ round, gaveUp }: { round: CountryPairRound; gaveUp: boolean }): React.JSX.Element {
  return (
    <>
      {round.countries.map((country) => (
        <AnswerOutline key={country.name} country={country} gaveUp={gaveUp} />
      ))}
    </>
  );
}

/**
 * Both countries as one shape: the same opaque fill and a stroke of the same colour, so nothing
 * shows where they meet.
 */
export function MergedShape({ round }: { round: CountryPairRound }): React.JSX.Element {
  return (
    <>
      {round.countries.map((country) => (
        <GeoJSON key={`${country.name}/merged`} data={country.outline} pathOptions={mergedShapeOptions()} />
      ))}
    </>
  );
}

/** The border the merged shape hides, drawn over it in a contrasting colour. */
export function HiddenBorder({ data, id }: { data: LineGeoData; id: string }): React.JSX.Element {
  return <GeoJSON key={id} data={data} pathOptions={hiddenBorderOptions()} />;
}
