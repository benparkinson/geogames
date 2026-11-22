import { normaliseString } from "../../helper/stringHelper";

/** What a game's hints say. How many there are comes from the backend: `round.hintCount`. */
export interface HintConfig {
  description: string;
  /**
   * Text to show for the hint at this index. Leave it out for hints that only change the map.
   * `foundAtHint[i]` is what the player had found when hint `i` was taken, for a hint that depends on it.
   */
  text?: (hintIndex: number, foundAtHint: string[][]) => string | null;
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
 * generic words ("Lake", "Rio") are skipped but never named: "Rio Negro" could give the answer away.
 */
export function firstLetterHint(name: string): string | null {
  const first = firstLetter(name);
  if (!first) {
    return null;
  }
  return first.prefix
    ? `Starts with “${first.letter}”, not counting words like “Lake” or “River”`
    : `Starts with “${first.letter}”`;
}

function significantWords(text: string): string[] {
  return text
    .split(/[\s-]+/)
    .filter((word) => word && !GENERIC_WORDS.has(word.toLowerCase()))
    .map(normaliseString)
    .filter((word) => word.length >= 3);
}

/**
 * Whether a hint that says `text` (the name of a place) would show a word of the answer's name. `names` is the
 * answer's name and its aliases. Generic words ("Lake", "of") and words under 3 letters are ignored, and Niger and
 * Nigeria count as the same word (a shared start of up to 5 letters, in words of at least 4).
 */
export function revealsName(text: string, names: string[]): boolean {
  const answerWords = names.flatMap(significantWords);
  return significantWords(text).some((word) =>
    answerWords.some((other) => {
      const shorter = Math.min(word.length, other.length);
      const share = Math.min(5, shorter);
      return shorter >= 4 && word.slice(0, share) === other.slice(0, share);
    }),
  );
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

/**
 * The text of a hint that says how long a name is: "Its name has 4 letters", or "Its name has 2 words: 5 and 5
 * letters". Counts letters only and splits words on whitespace and hyphens, so "Guinea-Bissau" is two words. Null for
 * a name with no letters.
 */
export function nameLengthHint(name: string): string | null {
  const lengths = name
    .split(/[\s-]+/)
    .map((word) => word.match(/\p{L}/gu)?.length ?? 0)
    .filter((n) => n > 0);
  if (lengths.length === 0) {
    return null;
  }
  if (lengths.length === 1) {
    return `Its name has ${lengths[0]} ${lengths[0] === 1 ? "letter" : "letters"}`;
  }
  const list = `${lengths.slice(0, -1).join(", ")} and ${lengths[lengths.length - 1]}`;
  return `Its name has ${lengths.length} words: ${list} letters`;
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
