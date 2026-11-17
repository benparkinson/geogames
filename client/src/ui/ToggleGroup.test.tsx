import { useState } from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ToggleGroup } from "./ToggleGroup";

const OPTIONS = [1, 3, 5].map(n => ({ value: n, label: String(n) }));

function Harness({ onChange }: { onChange: (n: number) => void }): React.JSX.Element {
  const [value, setValue] = useState(3);
  return (
    <>
      <span id="lbl">Rounds</span>
      <ToggleGroup name="rounds" value={value} options={OPTIONS} size="sm" aria-labelledby="lbl" onChange={n => { setValue(n); onChange(n); }} />
    </>
  );
}

describe("ToggleGroup", () => {
  it("is a labelled group of radios with the current value checked", () => {
    render(<Harness onChange={() => {}} />);

    expect(screen.getByRole("group", { name: "Rounds" })).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: "3" })).toBeChecked();
    expect(screen.getByRole("radio", { name: "5" })).not.toBeChecked();
    expect(screen.getByText("5")).toHaveClass("gg-btn", "gg-btn--sm");
  });

  it("calls onChange with the option's value on click", async () => {
    const onChange = vi.fn();
    render(<Harness onChange={onChange} />);

    await userEvent.click(screen.getByText("5"));

    expect(onChange).toHaveBeenCalledWith(5);
    expect(screen.getByRole("radio", { name: "5" })).toBeChecked();
  });

  it("moves with the arrow keys", async () => {
    const onChange = vi.fn();
    render(<Harness onChange={onChange} />);

    screen.getByRole("radio", { name: "3" }).focus();
    await userEvent.keyboard("{ArrowRight}");
    expect(onChange).toHaveBeenLastCalledWith(5);
    await userEvent.keyboard("{ArrowLeft}{ArrowLeft}");
    expect(onChange).toHaveBeenLastCalledWith(1);
  });
});
