import { SITE_NAME, SITE_URL } from "./siteName";

/** The pages with their own head tags; any other page gets only the site's description and link preview. */
export type PageKey = "site" | "daily" | "privacy" | "challenge" | "game";

/** One `<meta>` element. `key` is its name or property, so a page's tag replaces the site default. */
export interface MetaTag {
  key: string;
  name?: string;
  property?: string;
  content: string;
}

export interface PageHead {
  /** Absent on a game, whose title `GameRoundPage` sets as the round changes. */
  title?: string;
  tags: MetaTag[];
  /** Only on the pages search engines should list. */
  canonical?: string;
  jsonLd?: object[];
}

interface Page {
  path: string;
  title?: string;
  description: string;
  indexable: boolean;
}

const SITE_TITLE = `${SITE_NAME}: tricky geography puzzles and map quizzes`;

const SITE_DESCRIPTION =
  "Name countries from their shapes, borders, rivers and cities. " +
  "Take the daily challenge or play freely, as many rounds as you like.";

const PAGES: Record<PageKey, Page> = {
  site: { path: "/", title: SITE_TITLE, description: SITE_DESCRIPTION, indexable: true },
  daily: {
    path: "/daily",
    title: `Daily geography challenge | ${SITE_NAME}`,
    description: "Five tricky geography puzzles, the same for everyone today. How many can you solve?",
    indexable: true,
  },
  privacy: {
    path: "/privacy",
    title: `Credits and privacy | ${SITE_NAME}`,
    description: `The map data and software ${SITE_NAME} is built on, what it stores and who receives requests.`,
    indexable: true,
  },
  challenge: {
    path: "/challenge",
    // The page is static (the challenge is in ?id=), so the text can't name the score.
    title: `You've been challenged on ${SITE_NAME}`,
    description: "A friend played these geography puzzles. Can you beat their score?",
    indexable: false,
  },
  game: { path: "/", description: SITE_DESCRIPTION, indexable: false },
};

/** The paths of the pages search engines should list: what `public/sitemap.xml` holds. */
export const INDEXABLE_PATHS: string[] = Object.values(PAGES)
  .filter((page) => page.indexable)
  .map((page) => page.path);

/** Structured data telling search engines the site's name and logo. */
export function siteJsonLd(): object[] {
  const url = `${SITE_URL}/`;
  const context = "https://schema.org";
  return [
    {
      "@context": context,
      "@type": "WebSite",
      name: SITE_NAME,
      url,
      description: SITE_DESCRIPTION,
      inLanguage: "en",
    },
    { "@context": context, "@type": "Organization", name: SITE_NAME, url, logo: `${SITE_URL}/logo.png` },
  ];
}

/**
 * A page's head tags. They are read by crawlers that run no JavaScript, so they must not depend on
 * anything the client knows (the date, storage, the URL).
 */
export function pageMeta(page: PageKey): PageHead {
  const { path, title, description, indexable } = PAGES[page];
  const ogTitle = title ?? SITE_TITLE;
  const named = (name: string, content: string): MetaTag => ({ key: name, name, content });
  const property = (key: string, content: string): MetaTag => ({ key, property: key, content });
  const tags = [
    named("description", description),
    property("og:type", "website"),
    property("og:site_name", SITE_NAME),
    property("og:title", ogTitle),
    property("og:description", description),
    property("og:url", `${SITE_URL}${path}`),
    // Absolute: WhatsApp ignores a relative image.
    property("og:image", `${SITE_URL}/og-image.png`),
    property("og:image:width", "1200"),
    property("og:image:height", "630"),
    property("og:image:alt", `The ${SITE_NAME} logo: tricky geography puzzles, daily challenge and free play`),
    named("twitter:card", "summary_large_image"),
  ];
  if (!indexable) {
    tags.push(named("robots", "noindex"));
  }
  return {
    title,
    tags,
    canonical: indexable ? `${SITE_URL}${path}` : undefined,
    jsonLd: page === "site" ? siteJsonLd() : undefined,
  };
}
