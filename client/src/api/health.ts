import { serverEndpoint } from "./gateway";

/** Fire-and-forget request that starts waking a cold server. Failures are ignored. */
export function wakeServer(): void {
  fetch(serverEndpoint() + "/health", { mode: "no-cors" }).catch(() => { });
}
