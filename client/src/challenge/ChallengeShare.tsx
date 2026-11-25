import { useCreateChallenge } from "../api/challenges";
import type { GameRoundView } from "../api/types";
import ShareButton from "../daily/ShareButton";
import { resultFromGame } from "../daily/result";
import { Button, Notice, Spinner } from "../ui";
import { challengeShareText } from "./shareText";

/**
 * "Challenge a friend" for a finished game that isn't a daily. Mounting it makes the challenge (once per page
 * load), so by the time the player clicks the link is ready and the click can copy straight away.
 */
export default function ChallengeShare({ gameId, round }: { gameId: string; round: GameRoundView }): React.JSX.Element {
  const { data, isError } = useCreateChallenge(gameId);

  if (isError) {
    return <Notice>Couldn&apos;t make a challenge link</Notice>;
  }
  if (!data) {
    return (
      <Button variant="primary" disabled>
        <Spinner size="sm" decorative /> Challenge a friend
      </Button>
    );
  }
  const text = challengeShareText(resultFromGame(round.gameResult), data.id, window.location.origin);
  return <ShareButton text={text} label="Challenge a friend" />;
}
