import type { GameTypeName } from "./names";

/** A game with its own landing page: every game but Mixed. */
export type LandingType = Exclude<GameTypeName, "RANDOM">;

/** The words of a game's landing page, `/play/<slug>`. The game's name comes from `GAME_NAMES`. */
export interface Landing {
  /** The game's name in kebab-case. It is a public URL: don't change it. */
  slug: string;
  /** The `<title>`, at most 60 characters. */
  title: string;
  /** The meta description, at most 155 characters. */
  description: string;
  /** The page's h1: the game and the task. */
  heading: string;
  intro: string[];
  /** Never states hint counts or guess limits, which change. */
  howToPlay: string[];
}

const CLUES = "A wrong guess tells you how far off you are and in which direction, except on Hard.";
const HINTS = "Stuck? Take a hint. A hint, or a wrong guess, lowers what the round is worth.";

export const LANDING: Record<LandingType, Landing> = {
  TRIPOINT: {
    slug: "tripoints",
    title: "Tripoint quiz: name three countries | Geoggernaut",
    description:
      "A free map quiz about tripoints: a pin marks where three borders meet. Can you name all three countries?",
    heading: "Tripoints: name the three countries that meet at a point",
    intro: [
      "A tripoint is a place where the borders of three countries meet. You get a relief map with a pin on one.",
      "Mountains, rivers and coastlines are your clues, because the borders themselves are hidden until you need them.",
    ],
    howToPlay: [
      "Look at where the pin sits on the map.",
      "Type the countries you think meet there, in any order.",
      HINTS,
    ],
  },
  COUNTRIES_BY_CITIES: {
    slug: "city-map",
    title: "Guess the country from its cities | Geoggernaut",
    description:
      "A free city map quiz: every dot is a city, sized by its population. Name the country from the pattern.",
    heading: "City Map: guess the country from its cities",
    intro: [
      "There is no outline and no label, only a scatter of dots. Each one is a city, and bigger dots hold more people.",
      "Crowded river valleys, empty deserts and busy coasts all leave a pattern you can learn to read.",
    ],
    howToPlay: ["Study how the cities are spread out.", "Type the country they belong to.", CLUES, HINTS],
  },
  MERGED_COUNTRIES: {
    slug: "merged-countries",
    title: "Merged countries quiz: split the shape | Geoggernaut",
    description:
      "Two neighbouring countries are fused into a single shape with no border between them. Can you name both?",
    heading: "Merged Countries: name the two countries in one shape",
    intro: [
      "Take two neighbouring countries, rub out the border between them and fill the result with one colour.",
      "Your job is to spot which familiar outlines are hiding inside the unfamiliar one.",
    ],
    howToPlay: [
      "Look for coastlines and corners you recognise.",
      "Type both countries, in either order.",
      CLUES,
      HINTS,
    ],
  },
  COUNTRIES_BY_NEIGHBOURS: {
    slug: "missing-country",
    title: "Missing country quiz: name the gap | Geoggernaut",
    description: "The map shows a country's neighbours and leaves a gap where it should be. Which country is missing?",
    heading: "Missing Country: name the country between its neighbours",
    intro: [
      "Instead of the country itself, you see everything around it: the outlines of its neighbours and a gap.",
      "It is a good test of whether you know who borders whom, not just what each country looks like.",
    ],
    howToPlay: [
      "Look at the neighbouring outlines around the pin.",
      "Type the country that fills the gap.",
      CLUES,
      HINTS,
    ],
  },
  RIVERS_BY_COUNTRIES: {
    slug: "follow-the-river",
    title: "Name the river from its countries | Geoggernaut",
    description: "A river quiz: see the outlines of every country one river flows through, then name the river.",
    heading: "Follow the River: name the river that crosses these countries",
    intro: [
      "You see a handful of country outlines and nothing else. One river passes through every one of them.",
      "Work out where on Earth you are, then think about which waterway could link them all.",
    ],
    howToPlay: ["Work out which countries are shown.", "Type the river that flows through all of them.", HINTS],
  },
  COUNTRIES_BY_LAND_BORDERS: {
    slug: "land-borders",
    title: "Guess the country by its land borders | Geoggernaut",
    description:
      "Only the land borders of a country are drawn, with no coast and no neighbours. Can you tell which it is?",
    heading: "Land Borders: guess the country from its borders alone",
    intro: [
      "The coast is gone. What remains are the lines where one country ends and another begins.",
      "Long straight desert borders and winding river borders each tell their own story.",
    ],
    howToPlay: ["Picture the coast that would complete the lines.", "Type the country.", CLUES, HINTS],
  },
  COUNTRIES_BY_RIVERS: {
    slug: "waterways",
    title: "Guess the country by its rivers and lakes | Geoggernaut",
    description: "A map of rivers and lakes with the borders taken away. Can you name the country they run through?",
    heading: "Waterways: guess the country from its rivers and lakes",
    intro: [
      "Only the water is drawn: the rivers and lakes inside one country, floating on a blank map.",
      "Deltas, great lakes and the way streams branch can give a whole landscape away.",
    ],
    howToPlay: [
      "Follow where the rivers run and where they gather.",
      "Type the country they flow through.",
      CLUES,
      HINTS,
    ],
  },
  COUNTRIES_BY_COASTLINE: {
    slug: "coastlines",
    title: "Guess the country by its coastline | Geoggernaut",
    description: "Guess the country from its coastline alone. A free geography quiz of bays, peninsulas and islands.",
    heading: "Coastlines: guess the country from its coastline",
    intro: [
      "Here the land borders are left out, so all you have is the edge where a country meets the sea.",
      "Some coasts are unmistakable. Others will have you comparing every bay and headland you remember.",
    ],
    howToPlay: ["Look at the shape and direction of the coast.", "Type the country it belongs to.", CLUES, HINTS],
  },
  RIVERS_BY_SHAPE: {
    slug: "river-shapes",
    title: "Guess the river by its shape | Geoggernaut",
    description:
      "A hard river quiz: one river's course on a blank map, with no countries around it. Which river is it?",
    heading: "River Shapes: name a river from its shape",
    intro: [
      "A single winding line on an empty map: no borders, no cities and no sea to help you.",
      "This is one of the extra difficult games, for people who know the bends of the world's rivers.",
    ],
    howToPlay: ["Study the bends of the river's course.", "Type the name of the river.", HINTS],
  },
  LAKES_BY_SHAPE: {
    slug: "lake-shapes",
    title: "Guess the lake by its shape | Geoggernaut",
    description: "A hard lake quiz: the outline of a single lake on a blank map. Can you name it from its shape?",
    heading: "Lake Shapes: name a lake from its shape",
    intro: [
      "One body of water, cut out of its surroundings and shown alone.",
      "Long rift lakes, ragged reservoirs and round basins all look different once you pay attention.",
    ],
    howToPlay: ["Look closely at the outline of the lake.", "Type the name of the lake.", HINTS],
  },
  BORDERS_BY_SHAPE: {
    slug: "border-shapes",
    title: "Guess the border: which two countries? | Geoggernaut",
    description:
      "A hard border quiz: one line on the map divides two countries. Name both from the shape of the border.",
    heading: "Border Shapes: name the two countries a border separates",
    intro: [
      "You are given one international border and nothing else, just the line two countries share.",
      "It may follow a river, a mountain ridge or a ruler, and only its shape can tell you which pair it divides.",
    ],
    howToPlay: ["Examine the twists of the line.", "Type the two countries on either side of it.", CLUES, HINTS],
  },
};

/** The path of a game's landing page. */
export function landingPath(type: LandingType): string {
  return `/play/${LANDING[type].slug}`;
}

/** Every game with a landing page, in the home page's order. */
export const LANDING_TYPES = Object.keys(LANDING) as LandingType[];
