import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Spinner } from "./Spinner";
import { Notice } from "./Notice";

describe("Spinner", () => {
  it("is a status with hidden text by default", () => {
    render(<Spinner />);

    const status = screen.getByRole("status");
    expect(status).toHaveClass("gg-spinner");
    expect(status).not.toHaveClass("gg-spinner--sm");
    expect(status).toHaveTextContent("Loading…");
  });

  it("can be small", () => {
    render(<Spinner size="sm" />);

    expect(screen.getByRole("status")).toHaveClass("gg-spinner--sm");
  });

  it("is hidden from assistive technology when decorative", () => {
    const { container } = render(<Spinner decorative />);

    expect(screen.queryByRole("status")).toBeNull();
    expect(container.querySelector(".gg-spinner")).toHaveAttribute("aria-hidden", "true");
  });
});

describe("Notice", () => {
  it("is an alert in the danger tone by default", () => {
    render(<Notice className="mb-0">Oops</Notice>);

    const alert = screen.getByRole("alert");
    expect(alert).toHaveTextContent("Oops");
    expect(alert).toHaveClass("gg-notice", "gg-notice--danger", "mb-0");
  });
});
