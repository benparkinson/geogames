import { expect, test as base, type APIRequestContext, type Page } from "@playwright/test";

/** A place the player has to name, with the aliases that also count. */
export interface Answer {
  name: string;
  additionalNames: string[];
}

export interface RoundInfo {
  gameType: string;
  answers: Answer[];
}

/** The backend's own view of a game, so a test knows the right answers without scraping the page. */
export class Api {
  constructor(private readonly request: APIRequestContext) {}

  /**
   * Reads a round and works out its answers the way the frontend does (each game module's
   * `answerNames`): the three countries of a tripoint, the river, lake or country otherwise.
   */
  async round(gameId: string | number, round: number): Promise<RoundInfo> {
    const response = await this.request.get(`/api/games/${gameId}/rounds/${round}`);
    expect(response.ok(), `GET round ${round} of game ${gameId}`).toBe(true);
    const body = await response.json();
    const blob = JSON.parse(body.jsonBlob);
    const gameType: string = body.gameType.name;
    const places: Answer[] = gameType === "TRIPOINT" ? blob.countries : [blob.river ?? blob.lake ?? blob.country];
    return {
      gameType,
      answers: places.map(place => ({ name: place.name, additionalNames: place.additionalNames ?? [] })),
    };
  }

  /** Every country the guess box accepts (GET /api/countries). */
  async countries(): Promise<Answer[]> {
    const response = await this.request.get("/api/countries");
    expect(response.ok()).toBe(true);
    return response.json();
  }
}

const LOCAL_HOSTS = new Set(["localhost", "127.0.0.1", "[::1]"]);

function isLocal(url: URL): boolean {
  return LOCAL_HOSTS.has(url.hostname) || ["data:", "blob:", "about:"].includes(url.protocol);
}

export const test = base.extend<{ api: Api; guard: void }>({
  api: async ({ request }, use) => {
    await use(new Api(request));
  },

  // Runs for every test. Keeps the tests off the internet and fails them on errors in the page.
  guard: [
    async ({ context, page }, use) => {
      const blockedHosts: string[] = [];
      const problems: string[] = [];

      await context.route("**/*", route => {
        const url = new URL(route.request().url());
        if (isLocal(url)) {
          return route.continue();
        }
        // Map tiles, fonts and the like. Nothing a test needs, and nothing it may reach.
        blockedHosts.push(url.hostname);
        return route.abort();
      });

      page.on("pageerror", error => problems.push(`Uncaught error in the page: ${error.message}`));
      page.on("console", message => {
        if (message.type() !== "error") {
          return;
        }
        // Chromium logs "Failed to load resource" for every request we aborted above (the map
        // tiles). Only those are ignored: the same message for a local URL is a real failure.
        const failedLoad = message.text().startsWith("Failed to load resource");
        if (failedLoad && message.location().url && !isLocal(new URL(message.location().url))) {
          return;
        }
        problems.push(`console.error: ${message.text()}`);
      });

      await use();

      const production = blockedHosts.filter(
        host => host === "onrender.com" || host.endsWith(".onrender.com"),
      );
      expect(
        production,
        "The page called onrender.com. The bundled site must be built with NEXT_PUBLIC_API_URL=http://localhost:8080 " +
          "(see e2e/README.md), otherwise the tests would play against production.",
      ).toEqual([]);
      expect(problems, "Errors in the page").toEqual([]);
    },
    { auto: true },
  ],
});

export { expect };

/**
 * From the home page: chooses a game by its title and a number of rounds, starts it and returns the
 * game's id. The home page offers 3, 5 or 10 rounds.
 */
export async function startGame(page: Page, title: string, rounds = 3): Promise<string> {
  await page.goto("/");
  await page.getByRole("radio").filter({ has: page.getByText(title, { exact: true }) }).click();
  await page.getByRole("radio", { name: String(rounds), exact: true }).check({ force: true });
  await page.getByRole("button", { name: "Start game" }).click();
  await expect(page).toHaveURL(/\/games\/\d+$/);
  return page.url().split("/").pop()!;
}

/** Types a guess into the answer box and submits it. */
export async function guess(page: Page, text: string): Promise<void> {
  await answerBox(page).fill(text);
  await page.getByRole("button", { name: "Guess", exact: true }).click();
}

/** The box a guess is typed into. It has a datalist of suggestions, so its role is combobox, not textbox. */
export function answerBox(page: Page) {
  return page.locator("input.answer-attempt");
}
