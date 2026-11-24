import { QueryClientProvider } from "@tanstack/react-query";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { countryNames, makeRoundProps, makeTripointData } from "../../test/fixtures";
import { mockFetch } from "../../test/fetchMock";
import { renderWithClient } from "../../test/render";
import TripointGame from "./TripointGame";
import { hintedCountries, type TripointRoundData } from "./model";

vi.mock("./TripointMapWrapper", () => ({
  default: ({
    data,
    gameOver,
    gaveUp,
    hintsUsed,
    foundAtHint,
  }: {
    data: TripointRoundData;
    gameOver: boolean;
    gaveUp: boolean;
    hintsUsed: number;
    foundAtHint?: string[][];
  }) => (
    <div data-testid="map" data-game-over={String(gameOver)} data-gave-up={String(gaveUp)}>
      {hintedCountries(data.hintOrder, hintsUsed, foundAtHint).map((name) => (
        <span key={name} data-testid="hint-border">
          {name}
        </span>
      ))}
    </div>
  ),
}));

afterEach(() => {
  vi.restoreAllMocks();
});

function renderTripoint(tripoint = makeTripointData()) {
  const submitAnswer = vi.fn();
  const view = renderWithClient(
    <TripointGame data={tripoint} round={makeRoundProps()} answerCount={3} submitAnswer={submitAnswer} />,
  );
  return { ...view, tripoint, submitAnswer };
}

// Once the country list loads the input is a combobox, not a textbox.
const input = () => screen.getByLabelText("Country name…");

async function guess(user: ReturnType<typeof userEvent.setup>, text: string) {
  await user.type(input(), text + "{Enter}");
}

describe("Tripoint guessing", () => {
  it("prompts for the number of countries", () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    renderTripoint();

    expect(screen.getByText("Which 3 countries meet at this point?")).toBeInTheDocument();
  });

  it("accepts names, any case, and aliases, and reports CORRECT after the third", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    const user = userEvent.setup();
    const { submitAnswer } = renderTripoint();

    await guess(user, "Belgium");
    await guess(user, "holland");
    expect(submitAnswer).not.toHaveBeenCalled();

    await guess(user, "GERMANY");
    expect(submitAnswer).toHaveBeenCalledWith({
      answerState: "CORRECT",
      hintsUsed: 0,
      wrongGuesses: 0,
      answersFound: 3,
    });
  });

  it("treats a country that is not part of the tripoint as wrong", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    renderTripoint();

    await guess(userEvent.setup(), "France");

    expect(screen.getByText("France")).toBeInTheDocument();
  });

  it("gives no distance clue, because the map shows where the answers are", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    renderTripoint();

    await guess(userEvent.setup(), "France");

    expect(screen.queryByText(/ km /)).not.toBeInTheDocument();
  });

  it("does not check names against the country list until it has loaded", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise(() => {})),
    );
    renderTripoint();

    await guess(userEvent.setup(), "Atlantis");

    expect(screen.getByText("Atlantis")).toBeInTheDocument();
  });

  it("rejects a name that is not a country once the list has loaded, and accepts aliases", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    const user = userEvent.setup();
    const { client } = renderTripoint();
    await waitFor(() => expect(client.getQueryData(["placeNames", "countries"])).toBeDefined());

    await guess(user, "Atlantis");
    expect(screen.getByText(/We don't recognise "Atlantis"/)).toBeInTheDocument();

    await user.clear(input());
    await guess(user, "Deutschland");
  });
});

describe("Tripoint aliases", () => {
  it("counts a country once when it is guessed by alias and then by name", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    const user = userEvent.setup();
    renderTripoint();

    await guess(user, "Deutschland");
    expect(screen.getByRole("group", { name: "Answers: 1 of 3 found" })).toBeInTheDocument();
    await guess(user, "Germany");

    expect(screen.getByText("You've already guessed Germany.")).toBeInTheDocument();
    expect(screen.getByRole("group", { name: "Answers: 1 of 3 found" })).toBeInTheDocument();
  });

  it("shows a country found by alias under its own name", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    renderTripoint();

    await guess(userEvent.setup(), "holland");

    const slots = screen.getByRole("group", { name: "Answers: 1 of 3 found" });
    expect(slots).toHaveTextContent("Netherlands");
    expect(slots).not.toHaveTextContent("holland");
  });
});

describe("Tripoint hints", () => {
  it("shows one more country border per hint until none are left", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    vi.spyOn(Math, "random").mockReturnValue(0);
    const user = userEvent.setup();
    renderTripoint();
    expect(screen.queryAllByTestId("hint-border")).toHaveLength(0);

    await user.click(screen.getByRole("button", { name: /Hint .*· 3 left/ }));
    expect(screen.getAllByTestId("hint-border").map((e) => e.textContent)).toEqual(["Belgium"]);

    await user.click(screen.getByRole("button", { name: /Hint .*· 2 left/ }));
    expect(screen.getAllByTestId("hint-border").map((e) => e.textContent)).toEqual(["Belgium", "Netherlands"]);

    await user.click(screen.getByRole("button", { name: /Hint .*· 1 left/ }));
    expect(screen.getAllByTestId("hint-border").map((e) => e.textContent)).toEqual([
      "Belgium",
      "Netherlands",
      "Germany",
    ]);
    expect(screen.getByRole("button", { name: /Hint · 0 left/ })).toBeDisabled();
  });

  it("says in the panel that a missing country is outlined", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    vi.spyOn(Math, "random").mockReturnValue(0);
    renderTripoint();

    await userEvent.setup().click(screen.getByRole("button", { name: /Hint .*· 3 left/ }));

    expect(screen.getByText("A missing country is outlined on the map")).toBeInTheDocument();
  });

  it("names the outlined country once the player has found the rest", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    vi.spyOn(Math, "random").mockReturnValue(0);
    const user = userEvent.setup();
    renderTripoint();
    await guess(user, "Belgium");
    await guess(user, "Netherlands");

    await user.click(screen.getByRole("button", { name: /Hint .*· 3 left/ }));
    expect(screen.getByText("A missing country is outlined on the map")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Hint .*· 2 left/ }));
    expect(screen.getByText("Outlined on the map: Belgium, which you've already found")).toBeInTheDocument();
  });

  it("never shows the same country twice", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    vi.spyOn(Math, "random").mockReturnValue(0.99);
    const user = userEvent.setup();
    renderTripoint();

    await user.click(screen.getByRole("button", { name: /Hint .*· 3 left/ }));
    await user.click(screen.getByRole("button", { name: /Hint .*· 2 left/ }));

    const shown = screen.getAllByTestId("hint-border").map((e) => e.textContent);
    expect(new Set(shown).size).toBe(2);
  });

  it("does not modify the tripoint object it was given", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    vi.spyOn(Math, "random").mockReturnValue(0);
    const { tripoint } = renderTripoint();
    const before = structuredClone(tripoint);

    await userEvent.setup().click(screen.getByRole("button", { name: /Hint .*· 3 left/ }));

    expect(tripoint).toEqual(before);
  });

  it("keeps the same hint order when the round data is refetched", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    vi.spyOn(Math, "random").mockReturnValue(0);
    const user = userEvent.setup();
    const { rerender, client } = renderTripoint();
    await user.click(screen.getByRole("button", { name: /Hint .*· 3 left/ }));

    rerender(
      <QueryClientProvider client={client}>
        <TripointGame data={makeTripointData()} round={makeRoundProps()} answerCount={3} submitAnswer={vi.fn()} />
      </QueryClientProvider>,
    );

    expect(screen.getAllByTestId("hint-border").map((e) => e.textContent)).toEqual(["Belgium"]);
  });
});
