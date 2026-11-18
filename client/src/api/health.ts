import { useEffect } from "react";
import { serverEndpoint } from "./gateway";

/** Fire-and-forget request that starts waking a cold server. Failures are ignored. */
export function wakeServer(): void {
  fetch(serverEndpoint() + "/health", { mode: "no-cors" }).catch(() => {});
}

/** Starts waking a cold server when the page opens, while the player is still choosing or reading. */
export function useWakeServer(): void {
  useEffect(() => {
    wakeServer();
  }, []);
}
