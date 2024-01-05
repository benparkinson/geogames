import { QueryClientProvider } from "react-query";
import { ReactQueryDevtools } from "react-query/devtools";
import { client } from "../../../src/api/gateway";
import Tripoint from "../../../src/games/TripointRound";
import Head from "next/head";
import { useRouter } from "next/router";
import { useEffect, useState } from "react";

function TripointPage(params): JSX.Element {
    const router = useRouter()
    const [round, setRound] = useState(0)

    const gameId = router.query.gameId;

    function nextRound(): void {
        setRound(round + 1)
    }

    function prevRound(): void {
        setRound(round - 1)
    }

    // server to return 'hasNext' or 'roundcount' for the game probs?
    // creator of this page to pass the number of rounds

    return (
        <QueryClientProvider client={client}>
            <Head>
                <title>Tripoint | Geogames</title>
            </Head>
            <Tripoint gameId={gameId} round={round} nextRound={nextRound} prevRound={prevRound} />
            <ReactQueryDevtools initialIsOpen={false} />
        </QueryClientProvider>
    );
}

export default TripointPage;
