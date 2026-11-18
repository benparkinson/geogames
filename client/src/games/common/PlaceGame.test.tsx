import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { callsTo, mockFetch } from "../../test/fetchMock";
import { countryNames, makeRoundProps, riverNames } from "../../test/fixtures";
import { renderWithClient } from "../../test/render";
import PlaceGame from "./PlaceGame";

function MapStub() {
  return <div data-testid="map" />;
}

function renderPlaceGame(kind: "countries" | "rivers" | "lakes", hasClues: boolean) {
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
        round: makeRoundProps({ hasClues }),
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

  it("gives a wrong guess a clue when the round has clues, and the Hard mode switch", async () => {
    const user = userEvent.setup();
    renderPlaceGame("countries", true);
    await screen.findByRole("combobox", { name: "Country name…" });

    await user.type(screen.getByRole("combobox", { name: "Country name…" }), "France{Enter}");

    expect(screen.getAllByText(/[\d,]+ km/).length).toBeGreaterThan(0);
    expect(screen.getByRole("group", { name: "Hard mode" })).toBeInTheDocument();
  });

  it("gives no clue and no Hard mode switch when the round has none, even for countries", async () => {
    const user = userEvent.setup();
    renderPlaceGame("countries", false);
    await screen.findByRole("combobox", { name: "Country name…" });

    await user.type(screen.getByRole("combobox", { name: "Country name…" }), "France{Enter}");

    expect(screen.getByText("France")).toBeInTheDocument();
    expect(screen.queryByRole("group", { name: "Hard mode" })).not.toBeInTheDocument();
  });
});
