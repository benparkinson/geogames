import { useQuery } from "@tanstack/react-query";
import { queryKeys } from "./queryKeys";

/** Reads the deployed build id from this site's own origin (not the API server). Null if unreadable. */
export async function fetchLatestBuildId(): Promise<string | null> {
  const response = await fetch(`/version.json?t=${Date.now()}`, { cache: "no-store" });
  if (!response.ok) return null;
  const body: unknown = await response.json();
  if (typeof body !== "object" || body === null) return null;
  const buildId = (body as { buildId?: unknown }).buildId;
  return typeof buildId === "string" && buildId !== "" ? buildId : null;
}

/**
 * True when the server has a build whose id differs from the running bundle's. Off outside production
 * builds and when the bundle has no id.
 */
export function useUpdateAvailable(enabled = process.env.NODE_ENV === "production"): {
  updateAvailable: boolean;
  latestBuildId: string | null;
} {
  // Written in full so Next inlines it (see the comment in gateway.ts), and read inside the hook so
  // a test can stub it.
  const ownId = process.env.NEXT_PUBLIC_BUILD_ID;
  const { data } = useQuery({
    queryKey: queryKeys.appVersion(),
    queryFn: fetchLatestBuildId,
    enabled: enabled && !!ownId,
    refetchOnWindowFocus: true,
    refetchInterval: 15 * 60_000,
    refetchIntervalInBackground: false,
    staleTime: 5 * 60_000,
    retry: false,
  });
  const latestBuildId = data ?? null;
  return { updateAvailable: latestBuildId !== null && latestBuildId !== ownId, latestBuildId };
}
