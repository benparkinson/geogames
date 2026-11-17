import { useEffect } from "react";
import { useMap } from "react-leaflet";

/**
 * Adds the data credit and the privacy link to the map's attribution control, which every map shows
 * in its corner. `extra` is a credit only this map needs, shown first. The link is a plain `<a>`, so
 * it loads /privacy as a full page (the backend forwards that path to privacy.html).
 */
export function MapCredits({ extra }: { extra?: string }): null {
  const map = useMap();

  useEffect(() => {
    const html = (extra ? extra + " | " : "") + 'Map data: Natural Earth | <a href="/privacy">Privacy</a>';
    map.attributionControl.addAttribution(html);
    return () => {
      map.attributionControl.removeAttribution(html);
    };
  }, [map, extra]);

  return null;
}
