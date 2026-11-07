import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { countryNames, makeRoundProps, makeTripointData } from "../test/fixtures";
import { mockFetch } from "../test/fetchMock";
import { renderWithClient } from "../test/render";
import Tripoint, { TripointModel } from "./Tripoint";

vi.mock("../map/TripointMapWrapper", () => ({
  default: ({ data, gameOver, gaveUp }: { data: TripointModel; gameOver: boolean; gaveUp: boolean }) => (
    <div data-testid="map" data-game-over={String(gameOver)} data-gave-up={String(gaveUp)}>
      {data.countries
        .filter((c) => c.showAsHint)
        .map((c) => (
          <span key={c.name} data-testid="hint-border">
            {c.name}
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
    <Tripoint tripoint={tripoint} round={makeRoundProps()} answerCount={3} submitAnswer={submitAnswer} />,
  );
  return { ...view, tripoint, submitAnswer };
}

const input = () => screen.getByRole("textbox", { name: "Country name…" });

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
    expect(submitAnswer).toHaveBeenCalledWith("CORRECT");
  });

  it("treats a country that is not part of the tripoint as wrong", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    renderTripoint();

    await guess(userEvent.setup(), "France");

    expect(screen.getByText('"France" isn\'t right. Try again.')).toBeInTheDocument();
  });

  it("does not check names against the country list until it has loaded", async () => {
    vi.stubGlobal("fetch", vi.fn(() => new Promise(() => {})));
    renderTripoint();

    await guess(userEvent.setup(), "Atlantis");

    expect(screen.getByText('"Atlantis" isn\'t right. Try again.')).toBeInTheDocument();
  });

  it("rejects a name that is not a country once the list has loaded, and accepts aliases", async () => {
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    const user = userEvent.setup();
    const { client } = renderTripoint();
    await waitFor(() => expect(client.getQueryData(["countries"])).toBeDefined());

    await guess(user, "Atlantis");
    expect(screen.getByText(/We don't recognise "Atlantis"/)).toBeInTheDocument();

    await user.clear(input());
    await guess(user, "Deutschland");
    expect(screen.getByText("Correct! 2 to go.")).toBeInTheDocument();
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

  it("modifies the tripoint object it was given", async () => {
    // TODO: showRandomCountryBorder sets showAsHint on the country objects of the
    // `tripoint` prop itself instead of copying them first.
    mockFetch([{ url: /\/api\/countries$/, body: countryNames }]);
    vi.spyOn(Math, "random").mockReturnValue(0);
    const { tripoint } = renderTripoint();
    expect(tripoint.countries[0].showAsHint).toBe(false);

    await userEvent.setup().click(screen.getByRole("button", { name: /Hint \(3 left\)/ }));

    expect(tripoint.countries[0].showAsHint).toBe(true);
  });
});
