import { QueryClientProvider } from "@tanstack/react-query";
import { client } from "../src/api/gateway";
import HomePage from "../src/components/HomePage";
import Head from "next/head";

function MenuPage(): React.JSX.Element {
  return (
    <QueryClientProvider client={client}>
      <Head>
        <title>Geogames</title>
      </Head>
      <div id="root">
        <HomePage />
      </div>
    </QueryClientProvider>
  );
}

export default MenuPage;
