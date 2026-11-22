import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { GuessBox, placeList } from "./GuessBox";
import type { Suggestion } from "./suggestions";

describe("GuessBox", () => {
  it("uses its name as both label and placeholder", () => {
    render(<GuessBox value="" name="Country name…" onChange={vi.fn()} disabled={false} />);

    const input = screen.getByRole("textbox", { name: "Country name…" });
    expect(input).toHaveAttribute("placeholder", "Country name…");
  });

  it("reports typing through onChange as the new value", async () => {
    const onChange = vi.fn();
    render(<GuessBox value="" name="Guess" onChange={onChange} disabled={false} />);

    await userEvent.setup().type(screen.getByRole("textbox"), "a");

    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith("a");
  });

  it("can be disabled", () => {
    render(<GuessBox value="" name="Guess" onChange={vi.fn()} disabled />);

    expect(screen.getByRole("textbox")).toBeDisabled();
  });

  it("adds a class name to the input", () => {
    render(<GuessBox value="" name="Guess" onChange={vi.fn()} disabled={false} className="shake" />);

    expect(screen.getByRole("textbox")).toHaveClass("gg-input", "shake");
  });
});

const SUGGESTIONS: Suggestion[] = [
  { name: "Nepal", aliases: [] },
  { name: "Netherlands", aliases: ["Holland"] },
  { name: "New Zealand", aliases: [] },
  { name: "Niger", aliases: [] },
  { name: "Nigeria", aliases: [] },
  { name: "Nine Mile", aliases: [] },
];

function Harness({
  onChange = () => {},
  onSubmit = () => {},
  disabled,
}: {
  onChange?: (v: string) => void;
  onSubmit?: () => void;
  disabled?: boolean;
}) {
  const [value, setValue] = useState("");
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit();
      }}
    >
      <GuessBox
        value={value}
        name="Guess"
        disabled={disabled}
        suggestions={SUGGESTIONS}
        onChange={(v) => {
          setValue(v);
          onChange(v);
        }}
      />
      <button type="submit">Go</button>
    </form>
  );
}

const box = () => screen.getByRole("combobox");
const optionNames = () => screen.getAllByRole("option").map((o) => o.textContent);

describe("GuessBox suggestions", () => {
  it("is a plain textbox with no list without suggestions", () => {
    render(<GuessBox value="ne" name="Guess" onChange={vi.fn()} />);

    expect(screen.getByRole("textbox")).not.toHaveAttribute("aria-expanded");
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  });

  it("shows at most 5 options in rank order", () => {
    render(<Harness />);
    fireEvent.change(box(), { target: { value: "n" } });

    expect(optionNames()).toEqual(["Nepal", "Netherlands", "New Zealand", "Niger", "Nigeria"]);
  });

  it("shows the alias that matched", () => {
    render(<Harness />);
    fireEvent.change(box(), { target: { value: "hol" } });

    expect(optionNames()).toEqual(["NetherlandsHolland"]);
    expect(screen.getByText("Holland")).toHaveClass("gg-suggestion-alias");
  });

  it("offers longer names after an exact match", () => {
    render(<Harness />);
    fireEvent.change(box(), { target: { value: "Niger" } });

    expect(optionNames()).toEqual(["Niger", "Nigeria"]);
  });

  it("fills the box on click, closes the list, keeps the focus and does not submit", () => {
    const onChange = vi.fn();
    const onSubmit = vi.fn();
    render(<Harness onChange={onChange} onSubmit={onSubmit} />);
    box().focus();
    fireEvent.change(box(), { target: { value: "ne" } });
    const option = screen.getByRole("option", { name: "Netherlands" });

    // Cancelled, so the tap doesn't take the focus from the box.
    expect(fireEvent.mouseDown(option)).toBe(false);
    fireEvent.click(option);

    expect(onChange).toHaveBeenLastCalledWith("Netherlands");
    expect(box()).toHaveValue("Netherlands");
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
    expect(box()).toHaveFocus();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("picks the highlighted option on Enter without letting the form submit", () => {
    render(<Harness />);
    fireEvent.change(box(), { target: { value: "ne" } });

    // jsdom has no layout, so the list opens down: ArrowDown goes away from the box.
    fireEvent.keyDown(box(), { key: "ArrowDown" });
    const handled = fireEvent.keyDown(box(), { key: "Enter" });

    expect(handled).toBe(false);
    expect(box()).toHaveValue("Nepal");
  });

  it("leaves Enter to the form when nothing is highlighted", () => {
    render(<Harness />);
    fireEvent.change(box(), { target: { value: "ne" } });

    expect(fireEvent.keyDown(box(), { key: "Enter" })).toBe(true);
    expect(box()).toHaveValue("ne");
  });

  it("submits the form on Enter with nothing highlighted", async () => {
    const onSubmit = vi.fn();
    render(<Harness onSubmit={onSubmit} />);

    await userEvent.setup().type(box(), "ne{Enter}");

    expect(onSubmit).toHaveBeenCalledTimes(1);
  });

  it("moves the highlight with the arrows, clearing it past the first option", () => {
    render(<Harness />);
    fireEvent.change(box(), { target: { value: "ne" } });

    fireEvent.keyDown(box(), { key: "ArrowDown" });
    fireEvent.keyDown(box(), { key: "ArrowDown" });
    expect(screen.getByRole("option", { name: "Netherlands" })).toHaveAttribute("aria-selected", "true");
    fireEvent.keyDown(box(), { key: "ArrowUp" });
    fireEvent.keyDown(box(), { key: "ArrowUp" });

    expect(screen.getAllByRole("option").every((o) => o.getAttribute("aria-selected") === "false")).toBe(true);
    expect(box()).not.toHaveAttribute("aria-activedescendant");
  });

  it("keeps aria-expanded, aria-controls and aria-activedescendant in step with the list", () => {
    render(<Harness />);
    expect(box()).toHaveAttribute("aria-expanded", "false");

    fireEvent.change(box(), { target: { value: "ne" } });
    expect(box()).toHaveAttribute("aria-expanded", "true");
    expect(box()).toHaveAttribute("aria-controls", screen.getByRole("listbox").id);
    expect(box()).not.toHaveAttribute("aria-activedescendant");

    fireEvent.keyDown(box(), { key: "ArrowDown" });
    expect(box()).toHaveAttribute("aria-activedescendant", screen.getAllByRole("option")[0].id);
  });

  it("closes on Escape", () => {
    render(<Harness />);
    fireEvent.change(box(), { target: { value: "ne" } });

    fireEvent.keyDown(box(), { key: "Escape" });

    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  });

  it("closes on blur", () => {
    render(<Harness />);
    fireEvent.change(box(), { target: { value: "ne" } });

    fireEvent.blur(box());

    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  });

  // iOS Safari keeps the focus (and the keyboard) in the box when the player taps the map, so no blur comes.
  it("closes when the player taps outside the box, even where the tap's propagation is stopped", () => {
    render(
      <>
        <div data-testid="map" onPointerDown={(e) => e.stopPropagation()} />
        <Harness />
      </>,
    );
    box().focus();
    fireEvent.change(box(), { target: { value: "ne" } });
    expect(screen.getByRole("listbox")).toBeInTheDocument();

    fireEvent.pointerDown(screen.getByTestId("map"));

    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
    expect(box()).toHaveFocus();
    expect(box()).toHaveAttribute("aria-expanded", "false");
  });

  it("stays open when the player taps the box or an option", () => {
    render(<Harness />);
    fireEvent.change(box(), { target: { value: "ne" } });

    fireEvent.pointerDown(box());
    fireEvent.pointerDown(screen.getByRole("option", { name: "Nepal" }));

    expect(screen.getByRole("listbox")).toBeInTheDocument();
  });

  it("closes when the form is submitted, and opens again when the player types", () => {
    render(<Harness />);
    fireEvent.change(box(), { target: { value: "ne" } });

    fireEvent.submit(box().closest("form")!);
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();

    fireEvent.change(box(), { target: { value: "nep" } });
    expect(screen.getByRole("listbox")).toBeInTheDocument();
  });

  it("shows nothing when disabled", () => {
    render(<Harness disabled />);

    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  });

  it("shows nothing for an empty value or no match", () => {
    render(<Harness />);
    fireEvent.change(box(), { target: { value: "ne" } });

    fireEvent.change(box(), { target: { value: "" } });
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();

    fireEvent.change(box(), { target: { value: "zzz" } });
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  });

  it("still forwards the ref to the input", () => {
    const ref = { current: null as HTMLInputElement | null };
    render(<GuessBox ref={ref} value="" name="Guess" onChange={vi.fn()} suggestions={SUGGESTIONS} />);

    expect(ref.current).toBe(box());
  });
});

describe("placeList", () => {
  const rect = (top: number, height = 38) => ({ top, bottom: top + height, left: 10, width: 200 });

  it("opens down when there is room below", () => {
    const placed = placeList(rect(100), 800);

    expect(placed).toMatchObject({ direction: "down", top: 142, left: 10, width: 200, maxHeight: 654 });
  });

  it("opens up when the keyboard leaves no room below", () => {
    // A phone with the keyboard up: the visible viewport ends just under the box.
    const placed = placeList(rect(300), 360, undefined, 800);

    expect(placed).toMatchObject({ direction: "up", bottom: 504, maxHeight: 292 });
  });

  it("clamps the height to the larger side when neither fits", () => {
    const placed = placeList(rect(100), 230);

    expect(placed.direction).toBe("up");
    expect(placed.maxHeight).toBe(92);
  });
});
