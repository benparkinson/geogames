import { QueryClientProvider } from "react-query";
import { client } from "../../src/api/gateway";
import GameRoundPage from "../../src/games/common/GameRoundPage";
import { useRouter } from "next/router";

function TripointPage(params): JSX.Element {
    const router = useRouter();
    const { gameId } = router.query;

    return (
        <QueryClientProvider client={client}>
            {gameId && <GameRoundPage gameId={gameId} />}
        </QueryClientProvider>
    );
}

export default TripointPage;
