import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import PrivacyPage, { CONTACT_EMAIL, OWNER_NAME } from "./PrivacyPage";

describe("PrivacyPage", () => {
  it("has one Privacy heading", () => {
    render(<PrivacyPage />);

    expect(screen.getByRole("heading", { name: "Privacy", level: 1 })).toBeInTheDocument();
  });

  it("names who runs the site and how to reach them", () => {
    render(<PrivacyPage />);

    expect(screen.getByText(new RegExp(OWNER_NAME))).toBeInTheDocument();
    expect(screen.getByRole("link", { name: CONTACT_EMAIL }).getAttribute("href")).toBe("mailto:" + CONTACT_EMAIL);
  });

  it("links to the companies that receive the visitor's IP address", () => {
    render(<PrivacyPage />);

    expect(screen.getByRole("link", { name: "Render" }).getAttribute("href")).toBe("https://render.com/privacy");
    expect(screen.getByRole("link", { name: "EOX IT Services GmbH" }).getAttribute("href")).toBe("https://cloudless.eox.at");
  });

  it("says what is kept on the device", () => {
    render(<PrivacyPage />);

    expect(screen.getByText("gg-theme")).toBeInTheDocument();
    expect(screen.getByText("gg-hardcore")).toBeInTheDocument();
    expect(screen.getByText("gg-daily")).toBeInTheDocument();
    expect(screen.getByText(/up to three items/)).toBeInTheDocument();
  });

  it("shows the site footer", () => {
    render(<PrivacyPage />);

    expect(screen.getByRole("contentinfo")).toBeInTheDocument();
  });
});
