import Head from "next/head";
import "bootstrap/dist/css/bootstrap.css";
import "leaflet/dist/leaflet.css";
import "../src/styles.css";

export default function MyApp({ Component, pageProps }) {
  return (
    <>
      <Head>
        <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover, interactive-widget=resizes-content" />
      </Head>
      <Component {...pageProps} />
    </>
  );
}
