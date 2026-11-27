import { useChallengeTable, useCreateChallenge } from "../api/challenges";
import type { GameRoundView } from "../api/types";
import ShareButton from "../daily/ShareButton";
import { resultFromGame } from "../daily/result";
import { Button, Notice, Spinner } from "../ui";
import { challengePassOnText, challengeReplyText, challengeShareText } from "./shareText";

/**
 * The share buttons for a finished game that isn't a daily: "Challenge a friend" for whoever made the challenge,
 * "Reply" and "Pass it on" for whoever took it. Mounting it makes the challenge (once per page load), so by the
 * time the player clicks the link is ready and the click can copy straight away.
 */
export default function ChallengeShare({ gameId, round }: { gameId: string; round: GameRoundView }): React.JSX.Element {
  const { data, isError } = useCreateChallenge(gameId);
  // The league table says whether the player made the challenge or took it.
  const table = useChallengeTable(gameId, !!data);

  if (isError) {
    return <Notice>Couldn&apos;t make a challenge link</Notice>;
  }
  // Wait for the table too: the button must not change its meaning under the player's finger.
  if (!data || table.isLoading) {
    return (
      <Button variant="primary" disabled>
        <Spinner size="sm" decorative /> Challenge a friend
      </Button>
    );
  }
  const result = resultFromGame(round.gameResult);
  const own = table.data?.entries.find((e) => e.you) ?? table.data?.yours;
  // Without a table, the plain invitation still shares the right link.
  if (!table.data || table.data.creator || !own) {
    return <ShareButton text={challengeShareText(result, data.id)} label="Challenge a friend" />;
  }
  const { entries, entryCount } = table.data;
  return (
    <div className="gg-share__choices">
      <ShareButton text={challengeReplyText(result, own.rank, entryCount, data.id)} label="Reply" />
      <ShareButton
        text={challengePassOnText(result, entries[0].score, own.rank === 1, data.id)}
        label="Pass it on"
        variant="secondary"
      />
    </div>
  );
}
