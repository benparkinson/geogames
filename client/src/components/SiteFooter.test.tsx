import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import SiteFooter from "./SiteFooter";

const footer = () => within(screen.getByRole("contentinfo"));

describe("SiteFooter", () => {
  it("credits the data, imagery and icons, and links to the software licences", () => {
    render(<SiteFooter />);

    const href = (name: string) => footer().getByRole("link", { name }).getAttribute("href");
    expect(href("Natural Earth")).toBe("https://www.naturalearthdata.com/");
    expect(href("Wikidata")).toBe("https://www.wikidata.org/");
    expect(href("Wikipedia")).toBe("https://en.wikipedia.org/wiki/List_of_tripoints");
    expect(href("CC BY-SA 4.0")).toBe("https://creativecommons.org/licenses/by-sa/4.0/");
    expect(href("EOxCloudless")).toBe("https://cloudless.eox.at");
    expect(href("CC BY 4.0")).toBe("https://creativecommons.org/licenses/by/4.0/");
    expect(href("Earthz Stocker")).toContain("flaticon.com");
    expect(href("Freepik")).toContain("flaticon.com");
    expect(href("Software licences")).toBe("/third-party-licenses.txt");
    expect(footer().queryByText(/OpenStreetMap/)).not.toBeInTheDocument();
    expect(footer().queryByText(/Esri/)).not.toBeInTheDocument();
    expect(footer().getByText(/your light or dark mode choice/)).toBeInTheDocument();
  });

  it("says the tripoint positions were adjusted", () => {
    render(<SiteFooter />);

    expect(footer().getByText(/positions adjusted/)).toBeInTheDocument();
  });

  it("tells players that anonymous round results are recorded", () => {
    render(<SiteFooter />);

    expect(footer().getByText(/We record anonymous round results \(no cookies, no personal data\)/)).toBeInTheDocument();
  });

  it("links to the privacy page", () => {
    render(<SiteFooter />);

    expect(footer().getByRole("link", { name: "Privacy" }).getAttribute("href")).toBe("/privacy");
  });

  it("takes its spacing from the page", () => {
    render(<SiteFooter className="my-5" />);

    expect(screen.getByRole("contentinfo")).toHaveClass("my-5");
  });
});
