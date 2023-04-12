import { QueryClientProvider } from "react-query";
import { ReactQueryDevtools } from "react-query/devtools";
import { client } from "../src/api/gateway";
import RiverShapes from "../src/games/RiverShapes";
import Head from "next/head";

function RiverShapePage(): JSX.Element {
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
