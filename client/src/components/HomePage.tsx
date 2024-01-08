import Head from "next/head";
import Link from "next/link";
import { useMutation } from "react-query";
import { serverEndpoint } from "../api/gateway";
import isProduction from "../config/environment";
import { ignoreClick } from "../helper/buttonHelper";
import { Button } from "./Button";
import { useRouter } from "next/router";

function HomePage(): JSX.Element {
    const router = useRouter()
    const newTripointGameMutation = useMutation({
        mutationFn: () => fetch(serverEndpoint() + "/api/games?gameType=TRIPOINT", { method: "POST" }).then(result => result.json()),
        onSuccess: (data) => {
            const gameId = data.id;
            router.push("/games/" + gameId)
        }
    });
    const newRiverGameMutation = useMutation({
        mutationFn: () => fetch(serverEndpoint() + "/api/games?gameType=RIVERS_BY_SHAPE", { method: "POST" }).then(result => result.json()),
        onSuccess: (data) => {
            const gameId = data.id;
            router.push("/games/" + gameId)
        }
    });

    function createLink(pageName): string {
        if (isProduction()) {
            return "/" + pageName + ".html";
        } else {
            return "/" + pageName;
        }
    }

    function newTripointGame(event: React.MouseEvent<HTMLInputElement>): void {
        newTripointGameMutation.mutate();
    }

    function newRiverGame(event: React.MouseEvent<HTMLInputElement>): void {
        newRiverGameMutation.mutate();
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
                    <h6>Random round:</h6>
                    <div className="m-1">
                        <Link href={createLink("tripoint")}>
                            <Button text={"Tripoint"} bootstrapClass={"btn-info"} onClick={ignoreClick}></Button>
                        </Link>
                    </div>
                    <div className="m-1">
                        <Link href={createLink("rivershapes")}>
                            <Button text={"Rivers by Shape"} bootstrapClass={"btn-info"} onClick={ignoreClick}></Button>
                        </Link>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default HomePage;
