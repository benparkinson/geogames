import { QueryClientProvider } from "@tanstack/react-query";
import { client } from "../../src/api/gateway";
import GameRoundPage from "../../src/games/common/GameRoundPage";
import { useRouter } from "next/router";

function GameParentPage(): React.JSX.Element {
    const router = useRouter();
    const { gameId } = router.query;

    return (
        <QueryClientProvider client={client}>
            {gameId && <GameRoundPage key={gameId as string} gameId={gameId as string} />}
        </QueryClientProvider>
    );
}

export default GameParentPage;
