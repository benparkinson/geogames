/**
 * The name of every game, keyed by the backend GameType name: the only place a game's name is written.
 * The home page cards, the browser tab, the game's top bar and the results screen all show these.
 */
export const GAME_NAMES = {
  TRIPOINT: "Tripoints",
  RIVERS_BY_SHAPE: "River Shapes",
  RIVERS_BY_COUNTRIES: "Follow the River",
  COUNTRIES_BY_NEIGHBOURS: "Missing Country",
  COUNTRIES_BY_COASTLINE: "Coastlines",
  COUNTRIES_BY_LAND_BORDERS: "Land Borders",
  LAKES_BY_SHAPE: "Lake Shapes",
  BORDERS_BY_SHAPE: "Border Shapes",
  MERGED_COUNTRIES: "Merged Countries",
  COUNTRIES_BY_CITIES: "City Map",
  COUNTRIES_BY_RIVERS: "Waterways",
  /** The request-only type that mixes every game. */
  RANDOM: "Mixed",
} as const;

/** A backend GameType name, or the request-only RANDOM. */
export type GameTypeName = keyof typeof GAME_NAMES;
