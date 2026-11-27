import { serverEndpoint } from "./gateway";

/** A response that was not ok. `message` says what failed, for example "Failed to load round 3 (404)". */
export class HttpError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
  }
}

/** Whether `error` is a response with this HTTP status. */
export function hasStatus(error: unknown, status: number): boolean {
  return error instanceof HttpError && error.status === status;
}

/**
 * What to tell the player when creating a game failed: the server's limits (429, 503) have their own
 * messages, anything else gets `fallback`.
 */
export function creationErrorMessage(
  error: unknown,
  fallback = "Couldn't start a game. The server may be down. Try again.",
): string {
  if (hasStatus(error, 429)) return "Too many new games from your network. Wait a few minutes and try again.";
  if (hasStatus(error, 503)) return "The game server is busy. Try again later.";
  return fallback;
}

/**
 * Fetches `path` from the game server and parses the JSON response. When the response is not ok,
 * throws an `HttpError` that says what failed and the HTTP status, for example "Failed to load round 3 (404)".
 */
export async function fetchJson<T>(path: string, what: string, init?: RequestInit): Promise<T> {
  const response = await request(path, what, init);
  return response.json();
}

async function request(path: string, what: string, init?: RequestInit): Promise<Response> {
  const url = serverEndpoint() + path;
  const response = await (init ? fetch(url, init) : fetch(url));
  if (!response.ok) {
    throw new HttpError(`Failed to ${what} (${response.status})`, response.status);
  }
  return response;
}

function jsonRequest(method: string, body: unknown): RequestInit {
  return { method, headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) };
}

/** PUTs `body` to a path that answers with no content. Errors are those of `fetchJson`. */
export async function putJson(path: string, what: string, body: unknown): Promise<void> {
  await request(path, what, jsonRequest("PUT", body));
}

export function postJson<T>(path: string, what: string, body: unknown): Promise<T> {
  return fetchJson<T>(path, what, jsonRequest("POST", body));
}
