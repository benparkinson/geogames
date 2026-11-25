import { SITE_NAME } from "../siteName";
import { useState } from "react";
import { useRouter } from "next/router";
import { useUpdateAvailable } from "../api/version";
import { Button, Toast } from "../ui";

/** A wrapper so a test can replace the reload. */
export const pageActions = { reload: (): void => window.location.reload() };

/**
 * Offers a refresh when a newer build is deployed. Never shown on a game page: it would interrupt a
 * round, and the jar can't reload `/games/{id}`.
 */
export function UpdateNotice(): React.JSX.Element | null {
  const router = useRouter();
  const { updateAvailable, latestBuildId } = useUpdateAvailable();
  // Kept in memory only: a dismissal needs no storage.
  const [dismissedId, setDismissedId] = useState<string | null>(null);

  if (router.pathname === "/games/[gameId]") return null;
  if (!updateAvailable || latestBuildId === dismissedId) return null;
  return (
    <Toast
      onDismiss={() => setDismissedId(latestBuildId)}
      action={
        <Button variant="primary" size="sm" onClick={() => pageActions.reload()}>
          Refresh
        </Button>
      }
    >
      A new version of {SITE_NAME} is available.
    </Toast>
  );
}
