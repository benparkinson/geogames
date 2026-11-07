import GameRoundPage from "../../src/games/common/GameRoundPage";
import { useRouter } from "next/router";

function GameParentPage(): React.JSX.Element | null {
  const router = useRouter();
  const { gameId } = router.query;

  if (typeof gameId !== "string") {
    return null;
  }
  return <GameRoundPage key={gameId} gameId={gameId} />;
}

export default GameParentPage;
