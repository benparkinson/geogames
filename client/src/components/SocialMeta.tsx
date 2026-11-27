import Head from "next/head";
import { socialMeta, type SocialPage } from "../siteMeta";

/** The page's link preview tags. A page's own replace the site's from `_app`, which share their keys. */
export function SocialMeta({ page }: { page: SocialPage }): React.JSX.Element {
  return (
    <Head>
      {socialMeta(page).map(({ key, name, property, content }) => (
        <meta key={key} name={name} property={property} content={content} />
      ))}
    </Head>
  );
}
