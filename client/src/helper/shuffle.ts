/** A uniformly shuffled copy of the list (Fisher–Yates). The input is left alone. */
export function shuffle<T>(items: readonly T[]): T[] {
  const result = [...items];
  for (let i = 0; i < result.length - 1; i++) {
    const j = i + Math.floor(Math.random() * (result.length - i));
    [result[i], result[j]] = [result[j], result[i]];
  }
  return result;
}
