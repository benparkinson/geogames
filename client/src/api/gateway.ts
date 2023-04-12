import { QueryClient } from "react-query";
import isProduction from "../config/environment";

export const client: QueryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
    },
  },
});

export function serverEndpoint(): string {
  if (isProduction()) {
    return "";
  } else {
    return "http://localhost:8080";
  }
}
