import { QueryClientProvider } from "react-query";
import { ReactQueryDevtools } from "react-query/devtools";
import { client } from "../src/api/Gateway";
import Tripoint from "../src/components/Tripoint";
import Head from "next/head";

function TripointPage() {
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
