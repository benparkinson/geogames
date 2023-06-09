import { QueryClientProvider } from "react-query";
import { ReactQueryDevtools } from "react-query/devtools";
import { client } from "../src/api/gateway";
import Tripoint from "../src/games/Tripoint";
import Head from "next/head";

function TripointPage(): JSX.Element {
  return (
    <QueryClientProvider client={client}>
      <Head>
        <title>Tripoint | Geogames</title>
      </Head>
      <Tripoint />
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}

export default TripointPage;
