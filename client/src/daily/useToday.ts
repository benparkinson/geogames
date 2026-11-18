import { useSyncExternalStore } from "react";
import { localDateString } from "../api/daily";

const noSubscription = () => () => {};

/**
 * The player's local date, or null on the server and in the first client render. The page is pre-rendered at
 * build time, so nothing may read the date while rendering: the build's date would be baked into the HTML.
 */
export function useToday(): string | null {
  return useSyncExternalStore(noSubscription, localDateString, () => null);
}
