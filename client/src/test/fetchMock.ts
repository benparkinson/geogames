import { vi } from "vitest";

export interface Route {
  /** Defaults to GET. */
  method?: string;
  /** Matched against the full URL: a string must match exactly, a RegExp is tested. */
  url: string | RegExp;
  status?: number;
  /** JSON body, or a function that computes it from the request (called once per request). */
  body?: unknown | ((request: { url: string; init?: RequestInit }) => unknown);
  /** Make fetch itself reject, like a network error. */
  networkError?: boolean;
}

/**
 * Replaces global fetch with a router. A call with no matching route throws, so a test fails
 * loudly instead of silently hitting the network. vitest.setup.ts unstubs it after each test.
 */
export function mockFetch(routes: Route[]) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = (init?.method ?? "GET").toUpperCase();
    const route = routes.find(
      (r) =>
        (r.method ?? "GET").toUpperCase() === method &&
        (typeof r.url === "string" ? r.url === url : r.url.test(url)),
    );
    if (!route) {
      throw new Error(`Unrouted fetch: ${method} ${url}`);
    }
    if (route.networkError) {
      throw new TypeError("Failed to fetch");
    }
    const status = route.status ?? 200;
    const body = typeof route.body === "function" ? route.body({ url, init }) : route.body;
    return {
      ok: status >= 200 && status < 300,
      status,
      json: async () => body,
    } as Response;
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

/** The calls made to a mocked fetch as [method, url, parsed body] triples. */
export function callsTo(fetchMock: ReturnType<typeof mockFetch>) {
  return fetchMock.mock.calls.map(([input, init]) => ({
    method: (init?.method ?? "GET").toUpperCase(),
    url: String(input),
    body: typeof init?.body === "string" ? JSON.parse(init.body) : undefined,
    init,
  }));
}
