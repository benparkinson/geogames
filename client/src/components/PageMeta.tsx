import Head from "next/head";
import { pageMeta, type PageKey } from "../siteMeta";

/**
 * A page's head tags. A page's own replace the defaults from `_app`, which share their keys.
 *
 * `defaults` renders only the description and link preview tags: `next/head` can replace a tag but not
 * remove one, so a default title, canonical or structured data would end up on pages that must not have it.
 */
export function PageMeta({ page, defaults = false }: { page: PageKey; defaults?: boolean }): React.JSX.Element {
  const { title, tags, canonical, jsonLd } = pageMeta(page);
  return (
    <Head>
      {!defaults && title !== undefined && <title key="title">{title}</title>}
      {tags.map(({ key, name, property, content }) => (
        <meta key={key} name={name} property={property} content={content} />
      ))}
      {!defaults && canonical !== undefined && <link key="canonical" rel="canonical" href={canonical} />}
      {!defaults &&
        jsonLd?.map((data, index) => (
          <script
            key={`json-ld-${index}`}
            type="application/ld+json"
            // Escaped so that no string in the data can close the script element.
            dangerouslySetInnerHTML={{ __html: JSON.stringify(data).replace(/</g, "\\u003c") }}
          />
        ))}
    </Head>
  );
}
