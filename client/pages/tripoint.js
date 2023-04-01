import { QueryClientProvider } from "react-query";
import { ReactQueryDevtools } from "react-query/devtools";
import { client } from "../src/api/Gateway";
import Tripoint from "../src/components/Tripoint";

function TripointPage() {
  return (
    <QueryClientProvider client={client}>
      <Tripoint />
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}

export default TripointPage;
