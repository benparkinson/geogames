import type { AppProps } from "next/app";
import Head from "next/head";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { queryClient } from "../src/api/queryClient";
import ThemeToggle from "../src/components/ThemeToggle";
import "bootstrap/dist/css/bootstrap.css";
import "leaflet/dist/leaflet.css";
import "../src/styles.css";

export default function MyApp({ Component, pageProps }: AppProps): React.JSX.Element {
  return (
    <>
      <Head>
        <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover, interactive-widget=resizes-content" />
      </Head>
      <QueryClientProvider client={queryClient}>
        <ThemeToggle />
        <Component {...pageProps} />
        <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
    </>
  );
}
