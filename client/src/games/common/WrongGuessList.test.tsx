import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import WrongGuessList from "./WrongGuessList";

describe("WrongGuessList", () => {
  it("shows the distance and a hidden direction on the wrong-guess chip, unless clues are off", () => {
    const wrong = [{ text: "France", answer: null, clue: { km: 1236, bearing: 90 } }];
    const view = render(<WrongGuessList guesses={wrong} showClues={true} />);

    const chip = screen.getByText("France");
    expect(chip).toHaveTextContent(/1,240 km/);
    expect(screen.getByText("1,240 km east")).toBeInTheDocument();

    view.unmount();
    render(<WrongGuessList guesses={wrong} showClues={false} />);
    expect(screen.queryByText(/1,240 km/)).not.toBeInTheDocument();
  });

  it("lists each wrong guess", () => {
    render(
      <WrongGuessList
        guesses={[
          { text: "Italy", answer: null },
          { text: "Peru", answer: null },
        ]}
        showClues={true}
      />,
    );

    expect(screen.getByText("Wrong guesses")).toBeInTheDocument();
    expect(screen.getByText("Italy")).toBeInTheDocument();
    expect(screen.getByText("Peru")).toBeInTheDocument();
  });
});
