import { QueryClientProvider } from "react-query";
import { ReactQueryDevtools } from "react-query/devtools";
import "./App.css";
import { client } from "./api/Gateway";
import { Tripoint } from "./components/Tripoint";
import { setupMap } from "./map/MapHelper";

function App() {
  setupMap();

  return (
    <QueryClientProvider client={client}>
      <Tripoint />
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}

export default App;
