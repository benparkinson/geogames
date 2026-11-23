import { GeoJSON, Marker, Tooltip } from "react-leaflet";
import { targetIcon } from "../../map/markerIcons";
import { FitBounds } from "../../map/FitBounds";
import { boundsToLatLng } from "../../map/boundsToLatLng";
import {
  answerPathOptions,
  clueLineOptions,
  clueOutlineOptions,
  hintLineOptions,
  lakeFillOptions,
  minorRiverOptions,
} from "../../map/constants";
import type { CountryGeoData, LineGeoData } from "../common/country";
import type { CountryOutline, CountryShape } from "./model";

/** Fits the map to the focus (the largest part and big nearby islands), so overseas parts and the antimeridian don't zoom out to the world. */
export function FocusFit({ country }: { country: CountryShape }): React.JSX.Element {
  return <FitBounds id={country.name} bounds={boundsToLatLng(country.focus)} />;
}

/**
 * A pin in the middle of the country's largest part, so a gap between the neighbours can't be
 * mistaken for another one. It carries no name and draws no border, so the answer isn't given away.
 */
export function CountryPin({ country }: { country: CountryShape }): React.JSX.Element {
  const { south, west, north, east } = country.focus;
  return (
    <Marker
      key={`${country.name}/pin`}
      icon={targetIcon}
      position={[(south + north) / 2, (west + east) / 2]}
      interactive={false}
    />
  );
}

/** The filled outline of each neighbour. */
export function NeighbourOutlines({
  neighbours,
  id,
  labelled = false,
}: {
  neighbours: CountryOutline[];
  id: string;
  /** Show each neighbour's name on hover (once the round is over). */
  labelled?: boolean;
}): React.JSX.Element {
  return (
    <>
      {neighbours.map((neighbour) => (
        <GeoJSON key={`${id}/${neighbour.name}`} data={neighbour.outline} pathOptions={clueOutlineOptions()}>
          {labelled && <Tooltip sticky>{neighbour.name}</Tooltip>}
        </GeoJSON>
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
export function AnswerOutline({
  country,
  gaveUp,
  labelled = true,
}: {
  country: CountryShape;
  gaveUp: boolean;
  labelled?: boolean;
}): React.JSX.Element {
  return (
    <GeoJSON
      pathOptions={answerPathOptions(gaveUp)}
      data={country.outline}
      key={`${country.name}/outline${gaveUp ? "/gaveUp" : ""}`}
      // An unlabelled outline is drawn after the round's other layers, so it would otherwise catch the
      // hover meant for the dots beneath it (the city names).
      interactive={labelled}
    >
      {labelled && <Tooltip sticky>{country.name}</Tooltip>}
    </GeoJSON>
  );
}

/** The rivers of Country Rivers: the minor ones thin, the major ones over them in the clue line style. */
export function RiverLines({
  major,
  minor,
  id,
}: {
  major: LineGeoData;
  minor: LineGeoData;
  id: string;
}): React.JSX.Element {
  return (
    <>
      <GeoJSON key={`${id}/minor`} data={minor} pathOptions={minorRiverOptions()} />
      <GeoJSON key={`${id}/major`} data={major} pathOptions={clueLineOptions()} />
    </>
  );
}

/** Lakes a hint draws, filled. */
export function LakeFills({ data, id }: { data: CountryGeoData; id: string }): React.JSX.Element {
  return <GeoJSON key={id} data={data} pathOptions={lakeFillOptions()} />;
}
