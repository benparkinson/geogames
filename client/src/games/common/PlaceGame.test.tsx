import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { callsTo, mockFetch } from "../../test/fetchMock";
import { countryNames, makeRoundProps, riverNames } from "../../test/fixtures";
import { renderWithClient } from "../../test/render";
import PlaceGame from "./PlaceGame";

function MapStub() {
  return <div data-testid="map" />;
}

function renderPlaceGame(
  kind: "countries" | "rivers" | "lakes",
  hasClues: boolean,
  round = makeRoundProps({ hasClues }),
) {
  const fetchMock = mockFetch([
    { url: /\/api\/countries$/, body: countryNames },
    { url: /\/api\/rivers$/, body: riverNames },
    { url: /\/api\/lakes$/, body: [] },
  ]);
  renderWithClient(
    <PlaceGame
      kind={kind}
      game={{
        data: {},
        round,
        answerCount: 1,
        submitAnswer: vi.fn(),
      }}
      MapComponent={MapStub}
      answers={[{ name: "Netherlands", additionalNames: [] }]}
      prompt="Name it"

      hints={{ description: "A clue" }}
    />,
  );
  return fetchMock;
}

describe("PlaceGame", () => {
  it.each([
    ["countries", "Country name…"],
    ["rivers", "River name…"],
    ["lakes", "Lake name…"],
  ] as const)("asks for a %s by its own placeholder", (kind, placeholder) => {
    renderPlaceGame(kind, false);

    expect(screen.getByPlaceholderText(placeholder)).toBeInTheDocument();
  });

  it("loads the list of the kind it plays", async () => {
    const fetchMock = renderPlaceGame("rivers", false);

    await screen.findByRole("combobox", { name: "River name…" });

    const urls = callsTo(fetchMock).map((call) => call.url);
    expect(urls.some((url) => url.endsWith("/api/rivers"))).toBe(true);
    expect(urls.some((url) => url.endsWith("/api/countries"))).toBe(false);
  });

  it("gives a wrong guess a clue when the round has clues", async () => {
    const user = userEvent.setup();
    renderPlaceGame("countries", true);
    await screen.findByRole("combobox", { name: "Country name…" });

    await user.type(screen.getByRole("combobox", { name: "Country name…" }), "France{Enter}");

    expect(screen.getAllByText(/[\d,]+ km/).length).toBeGreaterThan(0);
  });

  it("gives no clue when the round has none, even for countries", async () => {
    const user = userEvent.setup();
    renderPlaceGame("countries", false);
    await screen.findByRole("combobox", { name: "Country name…" });

    await user.type(screen.getByRole("combobox", { name: "Country name…" }), "France{Enter}");

    expect(screen.getByText("France")).toBeInTheDocument();
  });

  describe("before the names have loaded", () => {
    function renderWith(fetchNames: () => Promise<Response>) {
      const round = makeRoundProps({});
      vi.stubGlobal("fetch", vi.fn(fetchNames));
      renderWithClient(
        <PlaceGame
          kind="countries"
          game={{ data: {}, round, answerCount: 1, submitAnswer: vi.fn() }}
          MapComponent={MapStub}
          answers={[{ name: "Netherlands", additionalNames: [] }]}
          prompt="Name it"
          hints={{ description: "A clue" }}
        />,
      );
      return round;
    }

    it("holds a guess back while the names are loading, then counts it once they have", async () => {
      const user = userEvent.setup();
      let release: (response: Response) => void = () => {};
      const round = renderWith(() => new Promise<Response>((resolve) => (release = resolve)));
      const input = screen.getByPlaceholderText("Country name…");

      await user.type(input, "Spaan{Enter}");

      expect(screen.getByText("Still loading the names. Try again in a moment.")).toBeInTheDocument();
      expect(round.recordGuess).not.toHaveBeenCalled();
      expect(input).toHaveValue("Spaan");

      release({ ok: true, status: 200, json: async () => countryNames } as Response);
      await screen.findByRole("combobox", { name: "Country name…" });
      await user.clear(input);
      await user.type(input, "France{Enter}");

      expect(round.recordGuess).toHaveBeenCalledWith({ text: "France", correct: false });
    });

    it("still takes a guess when the names failed to load", async () => {
      const user = userEvent.setup();
      const round = renderWith(async () => ({ ok: false, status: 500, json: async () => ({}) }) as Response);
      await waitFor(() => expect(screen.queryByText(/loading/i)).not.toBeInTheDocument());

      await user.type(screen.getByPlaceholderText("Country name…"), "Spaan{Enter}");

      await waitFor(() => expect(round.recordGuess).toHaveBeenCalledWith({ text: "Spaan", correct: false }));
    });
  });
});
