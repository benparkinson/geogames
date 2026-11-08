/** The text of a hint that gives the first letter of a name, or null for a blank name. */
export function firstLetterHint(name: string): string | null {
  const letter = name.trim().charAt(0).toUpperCase();
  return letter ? `Starts with “${letter}”` : null;
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
