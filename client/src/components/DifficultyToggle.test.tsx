import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { resetDifficultyForTests } from "../settings/difficulty";
import DifficultyToggle from "./DifficultyToggle";

beforeEach(() => {
  localStorage.clear();
  resetDifficultyForTests();
});

describe("DifficultyToggle", () => {
  it("is a group named Difficulty with Easy, Normal and Hard, on Normal to begin with", () => {
    render(<DifficultyToggle />);

    const group = screen.getByRole("group", { name: "Difficulty" });
    expect(
      within(group)
        .getAllByRole("radio")
        .map((r) => (r as HTMLInputElement).labels?.[0]?.textContent),
    ).toEqual(["Easy", "Normal", "Hard"]);
    expect(within(group).getByRole("radio", { name: "Normal" })).toBeChecked();
  });

  it("describes the selected difficulty", async () => {
    const user = userEvent.setup();
    render(<DifficultyToggle />);
    expect(screen.getByText("A distance and direction clue after each wrong guess.")).toBeInTheDocument();

    await user.click(screen.getByRole("radio", { name: "Easy" }));
    expect(screen.getByText("The continent is shown and you get more guesses.")).toBeInTheDocument();
    expect(screen.queryByText("A distance and direction clue after each wrong guess.")).not.toBeInTheDocument();

    await user.click(screen.getByRole("radio", { name: "Hard" }));
    expect(screen.getByText("No distance or direction clues after a wrong guess.")).toBeInTheDocument();
  });

  it("saves the choice and shows a saved one", async () => {
    const user = userEvent.setup();
    const { unmount } = render(<DifficultyToggle />);

    await user.click(screen.getByRole("radio", { name: "Hard" }));
    expect(localStorage.getItem("gg-difficulty")).toBe("hard");
    unmount();

    render(<DifficultyToggle />);
    expect(screen.getByRole("radio", { name: "Hard" })).toBeChecked();
  });

  it("shows an old Hard mode preference as Hard", () => {
    localStorage.setItem("gg-hardcore", "on");

    render(<DifficultyToggle />);

    expect(screen.getByRole("radio", { name: "Hard" })).toBeChecked();
  });
});
