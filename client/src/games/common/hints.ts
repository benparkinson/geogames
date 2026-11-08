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
  const words = name.trim().split(/\s+/).filter(Boolean);
  let skipped = 0;
  while (skipped < words.length - 1 && GENERIC_WORDS.has(words[skipped].toLowerCase())) {
    skipped++;
  }
  const letter = (words[skipped] ?? "").charAt(0).toUpperCase();
  if (!letter) {
    return null;
  }
  const prefix = words.slice(0, skipped).join(" ");
  return prefix ? `Starts with “${letter}” (after “${prefix}”)` : `Starts with “${letter}”`;
}

/**
 * "Label: A, B, C", naming at most `max` items and saying how many more there are.
 * Null for an empty list, so there is nothing to show.
 */
export function listHint(label: string, names: string[], max = 3): string | null {
  if (names.length === 0) {
    return null;
  }
  const named = names.slice(0, max);
  const more = names.length - named.length;
  const list = named.join(", ");
  return more === 0 ? `${label}: ${list}` : `${label}: ${list} and ${more} more`;
}

/** For example `countHint(1, "country", "countries", "Borders")` is "Borders 1 country". */
export function countHint(count: number, singular: string, plural: string, verb: string): string {
  return `${verb} ${count} ${count === 1 ? singular : plural}`;
}
