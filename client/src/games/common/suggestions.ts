import { normaliseString } from "../../helper/stringHelper";

export interface Suggestion {
  name: string;
  aliases: string[];
}

export interface SuggestionMatch {
  name: string;
  /** The alias that matched, when the name itself didn't. */
  alias?: string;
}

export interface PreparedSuggestion {
  name: string;
  normName: string;
  /** Each word of the name (split on spaces and hyphens), normalised. */
  words: string[];
  aliases: { alias: string; norm: string }[];
}

/** Normalises every name and alias once; call it in a useMemo. */
export function prepareSuggestions(suggestions: Suggestion[]): PreparedSuggestion[] {
  return suggestions.map(({ name, aliases }) => ({
    name,
    normName: normaliseString(name),
    words: name
      .split(/[\s-]+/)
      .map(normaliseString)
      .filter((w) => w !== ""),
    aliases: aliases.map((alias) => ({ alias, norm: normaliseString(alias) })).filter((a) => a.norm !== ""),
  }));
}

/** The tier (1 best) a suggestion reaches for the query, with the alias for the alias tiers; 0 for no match. */
function tierOf(query: string, s: PreparedSuggestion): { tier: number; alias?: string } {
  if (s.normName.startsWith(query)) return { tier: 1 };
  if (s.words.slice(1).some((w) => w.startsWith(query))) return { tier: 2 };
  const starting = s.aliases.find((a) => a.norm.startsWith(query));
  if (starting) return { tier: 3, alias: starting.alias };
  if (s.normName.includes(query)) return { tier: 4 };
  const containing = s.aliases.find((a) => a.norm.includes(query));
  if (containing) return { tier: 5, alias: containing.alias };
  return { tier: 0 };
}

/** The best matches for what the player typed, best first, at most `limit`. */
export function rankSuggestions(query: string, prepared: PreparedSuggestion[], limit = 5): SuggestionMatch[] {
  const q = normaliseString(query);
  if (q === "") return [];
  const tiers: SuggestionMatch[][] = [[], [], [], [], [], []];
  for (const s of prepared) {
    const { tier, alias } = tierOf(q, s);
    if (tier > 0) tiers[tier].push(alias === undefined ? { name: s.name } : { name: s.name, alias });
  }
  return tiers.flat().slice(0, limit);
}
