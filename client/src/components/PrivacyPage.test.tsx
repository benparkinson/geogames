import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import PrivacyPage, { CONTACT_EMAIL, OWNER_NAME } from "./PrivacyPage";

describe("PrivacyPage", () => {
  it("has one Credits and privacy heading", () => {
    render(<PrivacyPage />);

    expect(screen.getByRole("heading", { name: "Credits and privacy", level: 1 })).toBeInTheDocument();
  });

  it("names who runs the site and how to reach them", () => {
    render(<PrivacyPage />);

    expect(screen.getByText(new RegExp(OWNER_NAME))).toBeInTheDocument();
    expect(screen.getByRole("link", { name: CONTACT_EMAIL }).getAttribute("href")).toBe("mailto:" + CONTACT_EMAIL);
  });

  it("links to the companies that receive the visitor's IP address", () => {
    render(<PrivacyPage />);

    expect(screen.getByRole("link", { name: "Render" }).getAttribute("href")).toBe("https://render.com/privacy");
    expect(screen.queryByText(/EOX/)).not.toBeInTheDocument();
  });

  it("says what is kept on the device", () => {
    render(<PrivacyPage />);

    expect(screen.getByText("gg-theme")).toBeInTheDocument();
    expect(screen.getByText("gg-difficulty")).toBeInTheDocument();
    expect(screen.getByText("gg-daily")).toBeInTheDocument();
    expect(screen.getByText("gg-daily-streak")).toBeInTheDocument();
    expect(screen.getByText(/up to four items/)).toBeInTheDocument();
  });

  it("credits the data, imagery and icons, and links to the software licences", () => {
    render(<PrivacyPage />);

    const href = (name: string) => screen.getByRole("link", { name }).getAttribute("href");
    expect(href("Natural Earth")).toBe("https://www.naturalearthdata.com/");
    expect(href("Wikidata")).toBe("https://www.wikidata.org/");
    expect(href("Wikipedia")).toBe("https://en.wikipedia.org/wiki/List_of_tripoints");
    expect(href("CC BY-SA 4.0")).toBe("https://creativecommons.org/licenses/by-sa/4.0/");
    expect(href("Freepik")).toContain("flaticon.com");
    expect(href("Archivo")).toContain("fontsource.org");
    expect(href("IBM Plex Mono")).toContain("fontsource.org");
    expect(href("SIL Open Font Licence 1.1")).toBe("https://openfontlicense.org/");
    expect(href("Software licences")).toBe("/third-party-licenses.txt");
    expect(screen.getByText(/positions adjusted/)).toBeInTheDocument();
  });
});
