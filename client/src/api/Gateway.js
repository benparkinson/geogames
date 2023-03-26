import { QueryClient } from "react-query";

export const client = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
    },
  },
});

export const SERVER_ENDPOINT = "http://localhost:8080";
