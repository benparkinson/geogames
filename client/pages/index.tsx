import { QueryClientProvider } from "react-query";
import { client } from "../src/api/gateway";
import HomePage from "../src/components/HomePage";
import Head from "next/head";

function MenuPage(): JSX.Element {
  return (
    <QueryClientProvider client={client}>
      <Head>
        <title>Geogames!</title>
      </Head>
      <HomePage />
    </QueryClientProvider>
  );
}

export default MenuPage;
