import { QueryClientProvider } from "@tanstack/react-query";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { countryNames, makeRoundProps, makeTripointData } from "../../test/fixtures";
import { mockFetch } from "../../test/fetchMock";
import { renderWithClient } from "../../test/render";
import TripointGame from "./TripointGame";
import type { TripointRoundData } from "./model";

vi.mock("./TripointMapWrapper", () => ({
  default: ({
    data,
    gameOver,
    gaveUp,
    hintsUsed,
  }: {
    data: TripointRoundData;
    gameOver: boolean;
    gaveUp: boolean;
    hintsUsed: number;
  }) => (
    <div data-testid="map" data-game-over={String(gameOver)} data-gave-up={String(gaveUp)}>
      {data.hintOrder.slice(0, hintsUsed).map((name) => (
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

// Once the country list loads the input has a datalist, which makes its role "combobox".
const input = () => screen.getByLabelText("Country name…");

async function guess(user: ReturnType<typeof userEvent.setup>, text: string) {
  await user.type(input(), text + "{Enter}");
}

describe("Tripoint guessing", () => {
  it("prompts for the number of countries", () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    renderTripoint();

    expect(screen.getByText("Name the 3 countries that meet at the flag.")).toBeInTheDocument();
  });

  it("accepts names, any case, and aliases, and reports CORRECT after the third", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    const user = userEvent.setup();
    const { submitAnswer } = renderTripoint();

    await guess(user, "Belgium");
    expect(screen.getByText("Correct! 2 to go.")).toBeInTheDocument();
    await guess(user, "holland");
    expect(screen.getByText("Correct! 1 to go.")).toBeInTheDocument();
    expect(submitAnswer).not.toHaveBeenCalled();

    await guess(user, "GERMANY");
    expect(submitAnswer).toHaveBeenCalledWith({ answerState: "CORRECT", hintsUsed: 0 });
  });

  it("treats a country that is not part of the tripoint as wrong", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    renderTripoint();

    await guess(userEvent.setup(), "France");

    expect(screen.getByText('"France" isn\'t right. 4 guesses left.')).toBeInTheDocument();
  });

  it("does not check names against the country list until it has loaded", async () => {
    vi.stubGlobal("fetch", vi.fn(() => new Promise(() => {})));
    renderTripoint();

    await guess(userEvent.setup(), "Atlantis");

    expect(screen.getByText('"Atlantis" isn\'t right. 4 guesses left.')).toBeInTheDocument();
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
    expect(screen.getByText("Correct! 2 to go.")).toBeInTheDocument();
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

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    expect(screen.getAllByTestId("hint-border").map((e) => e.textContent)).toEqual(["Belgium"]);

    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));
    expect(screen.getAllByTestId("hint-border").map((e) => e.textContent)).toEqual(["Belgium", "Netherlands"]);

    await user.click(screen.getByRole("button", { name: /Hint \(1 left\)/ }));
    expect(screen.getAllByTestId("hint-border").map((e) => e.textContent)).toEqual([
      "Belgium",
      "Netherlands",
      "Germany",
    ]);
    expect(screen.getByRole("button", { name: /Hint \(0 left\)/ })).toBeDisabled();
  });

  it("never shows the same country twice", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    vi.spyOn(Math, "random").mockReturnValue(0.99);
    const user = userEvent.setup();
    renderTripoint();

    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));
    await user.click(screen.getByRole("button", { name: /Hint \(2 left\)/ }));

    const shown = screen.getAllByTestId("hint-border").map((e) => e.textContent);
    expect(new Set(shown).size).toBe(2);
  });

  it("does not modify the tripoint object it was given", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    vi.spyOn(Math, "random").mockReturnValue(0);
    const { tripoint } = renderTripoint();
    const before = structuredClone(tripoint);

    await userEvent.setup().click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));

    expect(tripoint).toEqual(before);
  });

  it("keeps the same hint order when the round data is refetched", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    vi.spyOn(Math, "random").mockReturnValue(0);
    const user = userEvent.setup();
    const { rerender, client } = renderTripoint();
    await user.click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));

    rerender(
      <QueryClientProvider client={client}>
        <TripointGame data={makeTripointData()} round={makeRoundProps()} answerCount={3} submitAnswer={vi.fn()} />
      </QueryClientProvider>,
    );

    expect(screen.getAllByTestId("hint-border").map((e) => e.textContent)).toEqual(["Belgium"]);
  });
});
