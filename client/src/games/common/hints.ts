/** What a game's hints say. How many there are comes from the backend: `round.hintCount`. */
export interface HintConfig {
  description: string;
  /** Text to show for the hint at this index. Leave it out for hints that only change the map. */
  text?: (hintIndex: number) => string | null;
}

/**
 * Words that say what kind of place a name is rather than which one, so a first-letter hint
 * skips them: "Lake Victoria" starts with "V", not "L". Includes other languages ("Rio Grande")
 * and the linking words that follow them ("Sea of Galilee").
 */
const GENERIC_WORDS = new Set([
  "lake",
  "lago",
  "lac",
  "loch",
  "lough",
  "river",
  "rio",
  "río",
  "sea",
  "of",
  "the",
  "de",
  "del",
]);

/**
 * The text of a hint that gives the first letter of a name, or null for a blank name. Leading
 * generic words ("Lake", "Rio") are skipped and mentioned, so the letter narrows the answer down.
 */
export function firstLetterHint(name: string): string | null {
  const first = firstLetter(name);
  if (!first) {
    return null;
  }
  return first.prefix ? `Starts with “${first.letter}” (after “${first.prefix}”)` : `Starts with “${first.letter}”`;
}

/**
 * The upper-cased first letter of a name and the generic leading words skipped to reach it ("" when
 * none were), or null for a blank name.
 */
export function firstLetter(name: string): { letter: string; prefix: string } | null {
  const words = name.trim().split(/\s+/).filter(Boolean);
  let skipped = 0;
  while (skipped < words.length - 1 && GENERIC_WORDS.has(words[skipped].toLowerCase())) {
    skipped++;
  }
  const letter = (words[skipped] ?? "").charAt(0).toUpperCase();
  return letter ? { letter, prefix: words.slice(0, skipped).join(" ") } : null;
}

/** For example `countHint(1, "country", "countries", "Borders")` is "Borders 1 country". */
export function countHint(count: number, singular: string, plural: string, verb: string): string {
  return `${verb} ${count} ${count === 1 ? singular : plural}`;
}

/** Builds the text of one hint. Returns null when there is nothing to say. */
export type HintBuilder<T> = (round: T) => string | null;

/** One hint per builder, revealed in order. */
export function hintsFrom<T>(description: string, round: T, ...builders: HintBuilder<T>[]): HintConfig {
  return { description, text: (index) => builders[index]?.(round) ?? null };
}

/** A hint whose text is always the same, for hints that change the map. */
export function mapHint<T>(text: string): HintBuilder<T> {
  return () => text;
}

export function cluesDescription(count: number): string {
  return "Shows " + (count === 1 ? "a clue" : `${count} clues, one at a time`);
}
