import { act, fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { GuessBox } from "./GuessBox";

describe("GuessBox", () => {
  it("uses its name as both label and placeholder", () => {
    render(<GuessBox value="" name="Country name…" onChange={vi.fn()} disabled={false} />);

    const input = screen.getByRole("textbox", { name: "Country name…" });
    expect(input).toHaveAttribute("placeholder", "Country name…");
  });

  it("reports typing through onChange", async () => {
    const onChange = vi.fn();
    render(<GuessBox value="" name="Guess" onChange={onChange} disabled={false} />);

    await userEvent.setup().type(screen.getByRole("textbox"), "a");

    expect(onChange).toHaveBeenCalledTimes(1);
  });

  it("can be disabled", () => {
    render(<GuessBox value="" name="Guess" onChange={vi.fn()} disabled />);

    expect(screen.getByRole("textbox")).toBeDisabled();
  });

  it("adds a class name to the input", () => {
    render(<GuessBox value="" name="Guess" onChange={vi.fn()} disabled={false} className="shake" />);

    expect(screen.getByRole("textbox")).toHaveClass("gg-input", "shake");
  });

  it("links the input to a datalist with one option per suggestion", () => {
    const { container } = render(
      <GuessBox value="" name="Guess" onChange={vi.fn()} disabled={false} suggestions={["France", "Spain"]} />,
    );

    const datalist = container.querySelector("datalist");
    expect(datalist).not.toBeNull();
    // An input with a list attribute is exposed as a combobox rather than a textbox.
    expect(screen.getByRole("combobox")).toHaveAttribute("list", datalist!.id);
    expect(Array.from(datalist!.querySelectorAll("option")).map((o) => o.value)).toEqual(["France", "Spain"]);
  });

  describe("focus after picking a suggestion", () => {
    afterEach(() => vi.useRealTimers());

    function type(inputType: string, value: string) {
      vi.useFakeTimers();
      render(<GuessBox value="" name="Guess" onChange={vi.fn()} disabled={false} suggestions={["France", "Spain"]} />);
      const input = screen.getByRole("combobox");
      fireEvent.input(input, { target: { value }, inputType });
      act(() => {
        vi.runAllTimers();
      });
      return input;
    }

    it("puts the focus back on the next tick when an option was picked from the list", () => {
      expect(type("insertReplacementText", "France")).toHaveFocus();
    });

    it("leaves the focus alone when the player typed a suggestion letter by letter", () => {
      expect(type("insertText", "France")).not.toHaveFocus();
    });

    it("leaves the focus alone when the text is not a suggestion", () => {
      expect(type("insertReplacementText", "Fran")).not.toHaveFocus();
    });
  });

  it("renders no datalist without suggestions", () => {
    const { container } = render(<GuessBox value="" name="Guess" onChange={vi.fn()} disabled={false} />);

    expect(container.querySelector("datalist")).toBeNull();
    expect(screen.getByRole("textbox")).not.toHaveAttribute("list");
  });
});
