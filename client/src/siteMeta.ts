import { SITE_NAME, SITE_URL } from "./siteName";

/** The pages with their own link preview; every other page gets the site's. */
export type SocialPage = "site" | "daily" | "challenge";

/** One `<meta>` element. `key` is its name or property, so a page's tag replaces the site default. */
export interface MetaTag {
  key: string;
  name?: string;
  property?: string;
  content: string;
}

const PAGES: Record<SocialPage, { path: string; title: string; description: string }> = {
  site: {
    path: "/",
    title: `${SITE_NAME}: tricky geography puzzles`,
    description:
      "Name countries from their shapes, borders, rivers and cities. " +
      "Take the daily challenge or play freely, as many rounds as you like.",
  },
  daily: {
    path: "/daily",
    title: `${SITE_NAME} daily challenge`,
    description: "Five tricky geography puzzles, the same for everyone today. How many can you solve?",
  },
  challenge: {
    path: "/challenge",
    // The page is static (the challenge is in ?id=), so the text can't name the score.
    title: `You've been challenged on ${SITE_NAME}`,
    description: "A friend played these geography puzzles. Can you beat their score?",
  },
};

/**
 * The description and the Open Graph and Twitter card tags of a page. They are read by crawlers that
 * run no JavaScript, so they must not depend on anything the client knows (the date, storage, the URL).
 */
export function socialMeta(page: SocialPage): MetaTag[] {
  const { path, title, description } = PAGES[page];
  const named = (name: string, content: string): MetaTag => ({ key: name, name, content });
  const property = (key: string, content: string): MetaTag => ({ key, property: key, content });
  return [
    named("description", description),
    property("og:type", "website"),
    property("og:site_name", SITE_NAME),
    property("og:title", title),
    property("og:description", description),
    property("og:url", `${SITE_URL}${path}`),
    // Absolute: WhatsApp ignores a relative image.
    property("og:image", `${SITE_URL}/og-image.png`),
    property("og:image:width", "1200"),
    property("og:image:height", "630"),
    property("og:image:alt", `The ${SITE_NAME} logo: tricky geography puzzles, daily challenge and free play`),
    named("twitter:card", "summary_large_image"),
  ];
}
