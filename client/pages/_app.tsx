import type { AppProps } from "next/app";
import Head from "next/head";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { queryClient } from "../src/api/queryClient";
import "bootstrap/dist/css/bootstrap.css";
import "leaflet/dist/leaflet.css";
import "../src/theme/tokens.css";
import "../src/theme/bootstrap-bridge.css";
import "../src/theme/components.css";
import "../src/styles.css";

export default function MyApp({ Component, pageProps }: AppProps): React.JSX.Element {
  return (
    <>
      <Head>
        <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover, interactive-widget=resizes-content" />
      </Head>
      <QueryClientProvider client={queryClient}>
        <Component {...pageProps} />
        <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
    </>
  );
}
