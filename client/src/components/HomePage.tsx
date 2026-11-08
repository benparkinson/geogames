import { useEffect, useRef, useState } from "react";
import { Alert, Button, Spinner, ToggleButton, ToggleButtonGroup } from "react-bootstrap";
import { wakeServer } from "../api/health";
import { useCreateGame } from "../api/games";
import { MENU_OPTIONS, MenuOption, ROUND_OPTIONS } from "../games/registry";

function HomePage(): React.JSX.Element {
    const { createGame, isLoading, error } = useCreateGame();
    const [selected, setSelected] = useState<string>(MENU_OPTIONS[0].type);
    const [rounds, setRounds] = useState(5);
    const [slow, setSlow] = useState(false);
    const cardRefs = useRef<(HTMLButtonElement | null)[]>([]);

    // Start waking a cold server while the player is still choosing.
    useEffect(() => {
        wakeServer();
    }, []);

    useEffect(() => {
        if (!isLoading) {
            return;
        }
        const timer = setTimeout(() => setSlow(true), 3000);
        return () => {
            clearTimeout(timer);
            setSlow(false);
        };
    }, [isLoading]);

    function handleKeyDown(e: React.KeyboardEvent, index: number): void {
        let next: number;
        if (e.key === "ArrowRight" || e.key === "ArrowDown") {
            next = (index + 1) % MENU_OPTIONS.length;
        } else if (e.key === "ArrowLeft" || e.key === "ArrowUp") {
            next = (index - 1 + MENU_OPTIONS.length) % MENU_OPTIONS.length;
        } else {
            return;
        }
        e.preventDefault();
        setSelected(MENU_OPTIONS[next].type);
        cardRefs.current[next]?.focus();
    }

    function renderCard(option: MenuOption, index: number): React.JSX.Element {
        const isSelected = option.type === selected;
        return (
            <div className="col-12 col-sm-4" key={option.type}>
                <button
                    type="button"
                    role="radio"
                    aria-checked={isSelected}
                    tabIndex={isSelected ? 0 : -1}
                    ref={el => { cardRefs.current[index] = el; }}
                    className={"game-card w-100 h-100 text-start rounded-3 p-3 " + (isSelected ? "border border-2 border-primary bg-primary-subtle" : "border bg-body")}
                    onClick={() => setSelected(option.type)}
                    onKeyDown={e => handleKeyDown(e, index)}
                >
                    <option.Icon size={28} aria-hidden="true" />
                    <div className="fw-bold mt-2">{option.title}</div>
                    <div className="small text-muted">{option.description}</div>
                </button>
            </div>
        );
    }

    return (
        <div className="min-vh-100 py-4 page-bg">
            <div className="container" style={{ maxWidth: 720 }}>
                <h1 className="text-center mt-3">Geogames</h1>
                <p className="lead text-center mb-4">Quick geography puzzles on a real map.</p>

                <h2 className="h5">Choose a game</h2>
                <div role="radiogroup" aria-label="Choose a game" className="row g-3 mb-4">
                    {MENU_OPTIONS.map(renderCard)}
                </div>

                <div className="d-flex align-items-center gap-3 mb-4">
                    <span id="rounds-label" className="h5 mb-0">Rounds</span>
                    <ToggleButtonGroup type="radio" name="rounds" value={rounds} onChange={(v: number) => setRounds(v)} aria-labelledby="rounds-label">
                        {ROUND_OPTIONS.map(n => (
                            <ToggleButton key={n} id={"rounds-" + n} value={n} variant="outline-primary">{n}</ToggleButton>
                        ))}
                    </ToggleButtonGroup>
                </div>

                <div className="text-center">
                    <Button
                        variant="primary"
                        size="lg"
                        className="w-100 w-sm-auto px-sm-5"
                        onClick={() => createGame(selected, rounds)}
                        disabled={isLoading}
                    >
                        {isLoading ? <><Spinner animation="border" size="sm" /> Starting…</> : "Start game"}
                    </Button>
                    <div aria-live="polite" className="mt-3">
                        {isLoading && slow && (
                            <p className="text-muted">Waking up the server. The first game can take up to a minute.</p>
                        )}
                        {!!error && !isLoading && (
                            <Alert variant="danger">Couldn&apos;t start a game. The server may be down. Try again.</Alert>
                        )}
                    </div>
                </div>

                <footer className="small text-muted text-center mt-5">
                    Icons: <a href="https://www.flaticon.com/free-icons/location-marker" title="location-marker icons">Earthz Stocker</a>,{" "}
                    <a href="https://www.flaticon.com/free-icons/earth-globe" title="earth globe icons">Freepik</a> (Flaticon) ·{" "}
                    River data © <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a> (ODbL) · River lines and lakes: <a href="https://www.naturalearthdata.com/">Natural Earth</a>
                </footer>
            </div>
        </div>
    );
}

export default HomePage;
