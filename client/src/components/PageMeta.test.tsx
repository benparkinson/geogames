import { render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { pageMeta } from "../siteMeta";
import { PageMeta } from "./PageMeta";

// Renders the head's children in place, so a test can look at them.
vi.mock("next/head", () => ({ default: ({ children }: { children: React.ReactNode }) => <>{children}</> }));

const canonical = () => document.querySelector('link[rel="canonical"]');
const jsonLd = () => [...document.querySelectorAll('script[type="application/ld+json"]')];
const robots = () => document.querySelector('meta[name="robots"]');

describe("PageMeta", () => {
  it("renders the title, canonical link and structured data of the home page", () => {
    render(<PageMeta page="site" />);

    expect(document.title).toBe(pageMeta("site").title);
    expect(canonical()?.getAttribute("href")).toBe(pageMeta("site").canonical);
    expect(jsonLd().map((script) => JSON.parse(script.textContent ?? ""))).toEqual(pageMeta("site").jsonLd);
    expect(robots()).toBeNull();
  });

  it("keeps a page that is not indexable out of search results", () => {
    render(<PageMeta page="challenge" />);

    expect(robots()?.getAttribute("content")).toBe("noindex");
    expect(canonical()).toBeNull();
  });

  it("leaves a game's title to the game", () => {
    render(<PageMeta page="game" />);

    expect(document.querySelector("title")).toBeNull();
  });

  it("renders only the meta tags as the default", () => {
    render(<PageMeta page="site" defaults />);

    expect(document.querySelector('meta[name="description"]')).not.toBeNull();
    expect(document.querySelector("title")).toBeNull();
    expect(canonical()).toBeNull();
    expect(jsonLd()).toEqual([]);
  });
});
