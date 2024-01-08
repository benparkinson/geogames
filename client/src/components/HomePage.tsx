import { useMutation } from "react-query";
import { serverEndpoint } from "../api/gateway";
import { Button } from "./Button";
import { useRouter } from "next/router";

function HomePage(): JSX.Element {
    const router = useRouter();
    const newGameMutation = useMutation((gameType: string) =>
        fetch(serverEndpoint() + "/api/games?gameType=" + gameType, { method: "POST" }).then(result => result.json()),
        {
            onSuccess: (data: { id: any; }) => {
                const gameId = data.id;
                router.push("/games/" + gameId)
            }
        }
    );

    function newTripointGame(event: React.MouseEvent<HTMLInputElement>): void {
        newGameMutation.mutate("TRIPOINT");
    }

    function newRiverGame(event: React.MouseEvent<HTMLInputElement>): void {
        newGameMutation.mutate("RIVERS_BY_SHAPE");
    }

    return (
        <div className="d-flex vh-100">
            <div className="container d-flex justify-content-center align-items-center text-center">
                <div className="row d-flex flex-column">
                    <h4>Welcome to Geogames (working title)</h4>
                    <h6>New game (5 rounds):</h6>
                    <div className="m-1">
                        <Button text={"Tripoint"} bootstrapClass={"btn-info"} onClick={newTripointGame}></Button>
                    </div>
                    <div className="m-1">
                        <Button text={"Rivers"} bootstrapClass={"btn-info"} onClick={newRiverGame}></Button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default HomePage;
