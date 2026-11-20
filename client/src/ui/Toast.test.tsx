import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Toast } from "./Toast";

describe("Toast", () => {
  it("shows its message and action in a status region", () => {
    render(
      <Toast onDismiss={() => {}} action={<button>Go</button>}>
        Hello there
      </Toast>,
    );
    expect(screen.getByRole("status").textContent).toContain("Hello there");
    expect(screen.getByRole("button", { name: "Go" })).toBeTruthy();
  });

  it("calls onDismiss from the close button", async () => {
    const onDismiss = vi.fn();
    render(<Toast onDismiss={onDismiss}>Hello</Toast>);
    await userEvent.click(screen.getByRole("button", { name: "Dismiss" }));
    expect(onDismiss).toHaveBeenCalledOnce();
  });
});
