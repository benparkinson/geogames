import { QueryClient } from "react-query";
import isProduction from "../config/environment";

export const client = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
    },
  },
});

export function serverEndpoint() {
  if (isProduction()) {
    return "";
  } else {
    return "http://localhost:8080";
  }
}
