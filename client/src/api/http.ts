import { serverEndpoint } from "./gateway";

/**
 * Fetches `path` from the game server and parses the JSON response. When the response is not ok,
 * throws an Error that says what failed and the HTTP status, for example "Failed to load round 3 (404)".
 */
export async function fetchJson<T>(path: string, what: string, init?: RequestInit): Promise<T> {
  const url = serverEndpoint() + path;
  const response = await (init ? fetch(url, init) : fetch(url));
  if (!response.ok) {
    throw new Error(`Failed to ${what} (${response.status})`);
  }
  return response.json();
}

export function postJson<T>(path: string, what: string, body: unknown): Promise<T> {
  return fetchJson<T>(path, what, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}
