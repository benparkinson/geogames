import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Button, buttonClass } from "./Button";

describe("buttonClass", () => {
  it("defaults to the medium secondary button", () => {
    expect(buttonClass()).toBe("gg-btn gg-btn--secondary");
  });

  it.each(["primary", "secondary", "hint", "danger", "quiet"] as const)("adds the %s variant class", variant => {
    expect(buttonClass({ variant })).toContain(`gg-btn--${variant}`);
  });

  it("adds size, block, pill and extra classes", () => {
    expect(buttonClass({ variant: "quiet", size: "sm", pill: true, className: "x" })).toBe("gg-btn gg-btn--quiet gg-btn--sm gg-btn--pill x");
    expect(buttonClass({ size: "lg", block: true })).toBe("gg-btn gg-btn--secondary gg-btn--lg gg-btn--block");
  });
});

describe("Button", () => {
  it("is a type=button button by default", () => {
    render(<Button>Go</Button>);

    const button = screen.getByRole("button", { name: "Go" });
    expect(button).toHaveAttribute("type", "button");
    expect(button).toHaveClass("gg-btn", "gg-btn--secondary");
  });

  it("applies the variant and size", () => {
    render(<Button variant="primary" size="lg" block className="extra">Go</Button>);

    expect(screen.getByRole("button")).toHaveClass("gg-btn--primary", "gg-btn--lg", "gg-btn--block", "extra");
  });

  it("forwards props and clicks, and ignores clicks when disabled", async () => {
    const onClick = vi.fn();
    const { rerender } = render(<Button type="submit" title="Hi" aria-label="Go now" onClick={onClick}>Go</Button>);

    const button = screen.getByRole("button", { name: "Go now" });
    expect(button).toHaveAttribute("type", "submit");
    expect(button).toHaveAttribute("title", "Hi");
    await userEvent.click(button);
    expect(onClick).toHaveBeenCalledTimes(1);

    rerender(<Button disabled onClick={onClick}>Go</Button>);
    await userEvent.click(screen.getByRole("button"));
    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
