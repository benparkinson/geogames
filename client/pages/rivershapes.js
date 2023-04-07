import { QueryClientProvider } from "react-query";
import { ReactQueryDevtools } from "react-query/devtools";
import { client } from "../src/api/Gateway";
import RiverShapes from "../src/games/RiverShapes";
import Head from "next/head";

function RiverShapePage() {
  return (
    <QueryClientProvider client={client}>
      <Head>
        <title>Rivers by Shape | Geogames</title>
      </Head>
      <RiverShapes />
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}

export default RiverShapePage;
