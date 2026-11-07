import { GeoJSON, Marker, TileLayer, Tooltip } from "react-leaflet";
import { endFlagIcon, startFlagIcon } from "../../map/flagIcons";
import { answerPathOptions, clueOutlineOptions, SATELLITE_TILE_URL } from "../../map/constants";
import type { Country } from "../common/country";
import type { River } from "./model";

/** The river's line. Re-created when the player gives up so the colour changes. */
export function RiverLine({ river, gaveUp }: { river: River; gaveUp: boolean }): React.JSX.Element {
  return <GeoJSON pathOptions={answerPathOptions(gaveUp)} data={river.geoData} key={river.id + (gaveUp ? "gaveUp" : "")} />;
}

/** Satellite imagery, shown once the round is over. Its tiles carry no labels. */
export function RevealTiles({ show }: { show: boolean }): React.JSX.Element | null {
  return show ? <TileLayer url={SATELLITE_TILE_URL} /> : null;
}

/** A green flag where the river starts and a red one where it ends. */
export function EndFlags({ river }: { river: River }): React.JSX.Element {
  return (
    <>
      <Marker icon={startFlagIcon} position={[river.source.latitude, river.source.longitude]}>
        <Tooltip>Start</Tooltip>
      </Marker>
      <Marker icon={endFlagIcon} position={[river.mouth.latitude, river.mouth.longitude]}>
        <Tooltip>End</Tooltip>
      </Marker>
    </>
  );
}

/** The outline of each country the river flows through. */
export function CountryOutlines({ countries }: { countries: Country[] }): React.JSX.Element {
  return (
    <>
      {countries.map((country) => (
        <GeoJSON key={country.name} data={country.geoData} pathOptions={clueOutlineOptions()} />
      ))}
    </>
  );
}
