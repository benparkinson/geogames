import { PageMeta } from "../../src/components/PageMeta";
import GameRoundPage from "../../src/games/common/GameRoundPage";
import { useRouter } from "next/router";

function GameParentPage(): React.JSX.Element {
  const router = useRouter();
  const { gameId } = router.query;

  return (
    <>
      <PageMeta page="game" />
      {/* The id is unknown in the pre-rendered page. */}
      {typeof gameId === "string" && <GameRoundPage key={gameId} gameId={gameId} />}
    </>
  );
}

export default GameParentPage;
