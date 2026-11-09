import { useEffect, useRef, useState } from "react";
import { Button, Notice, Spinner, ToggleGroup } from "../ui";
import { wakeServer } from "../api/health";
import { useCreateGame } from "../api/games";
import { Fire } from "react-bootstrap-icons";
import KofiBadge from "./KofiBadge";
import ThemeToggle from "./ThemeToggle";
import { MENU_OPTIONS, MenuOption, ROUND_OPTIONS } from "../games/registry";

const STANDARD_OPTIONS = MENU_OPTIONS.filter(option => !option.extraDifficult);
const EXTRA_DIFFICULT_OPTIONS = MENU_OPTIONS.filter(option => option.extraDifficult);

function HomePage(): React.JSX.Element {
    const { createGame, isLoading, error } = useCreateGame();
    const [selected, setSelected] = useState<string | null>(null);
    const selectedOption = MENU_OPTIONS.find(o => o.type === selected);
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

    function renderCard(option: MenuOption): React.JSX.Element {
        // The position in the full list, which the arrow keys and focus refs use.
        const index = MENU_OPTIONS.indexOf(option);
        const isSelected = option.type === selected;
        return (
            <div className="col-12 col-sm-4" key={option.type}>
                <button
                    type="button"
                    role="radio"
                    aria-checked={isSelected}
                    tabIndex={(selected === null ? index === 0 : isSelected) ? 0 : -1}
                    ref={el => { cardRefs.current[index] = el; }}
                    className={"game-card w-100 h-100 text-start p-3" + (isSelected ? " game-card--selected" : "")}
                    onClick={() => setSelected(option.type)}
                    onKeyDown={e => handleKeyDown(e, index)}
                >
                    <option.Icon size={28} aria-hidden="true" />
                    <div className="fw-bold mt-2">{option.title}</div>
                    <div className="small gg-text-muted">{option.description}</div>
                </button>
            </div>
        );
    }

    return (
        <div className={"min-vh-100 py-4 page-bg" + (selectedOption ? " home--selected" : "")}>
            <header className="home-header">
                <div className="d-flex justify-content-end"><ThemeToggle /></div>
                <h1 className="text-center mt-3">GeoGauntlet</h1>
                <p className="lead text-center mb-4">Quick but challenging geography puzzles.</p>
            </header>
            <div className="home-layout">
                <section className="home-main">
                <h2 className="h5">Choose a game</h2>
                {/* One radio group across both sections, so the arrow keys move through every card. */}
                <div role="radiogroup" aria-label="Choose a game" className="mb-4">
                    <div className="row g-3">
                        {STANDARD_OPTIONS.map(renderCard)}
                    </div>
                    <div role="group" aria-labelledby="extra-difficult-label" className="mt-4">
                        <h3 id="extra-difficult-label" className="h6 d-flex align-items-center gap-2 mb-1">
                            <Fire className="gg-text-danger" aria-hidden="true" /> Extra difficult
                        </h3>
                        <p className="small gg-text-muted mb-2">Just a shape on a blank map, with nothing around it.</p>
                        <div className="row g-3">
                            {EXTRA_DIFFICULT_OPTIONS.map(renderCard)}
                        </div>
                    </div>
                </div>
                </section>

                {selectedOption && (
                    <aside className="home-start" aria-label="Start a game">
                        <div className="d-flex flex-wrap align-items-center gap-2">
                            <div className="d-flex align-items-center gap-2 w-100">
                                <selectedOption.Icon size={20} aria-hidden="true" />
                                <span className="fw-bold">{selectedOption.title}</span>
                            </div>
                            <div className="d-flex align-items-center gap-2">
                                <span id="rounds-label" className="fw-semibold">Rounds</span>
                                <ToggleGroup
                                    size="sm"
                                    name="rounds"
                                    value={rounds}
                                    onChange={setRounds}
                                    aria-labelledby="rounds-label"
                                    options={ROUND_OPTIONS.map(n => ({ value: n, label: n }))}
                                />
                            </div>
                            <Button
                                variant="primary"
                                className="home-start-button flex-grow-1"
                                onClick={() => createGame(selectedOption.type, rounds)}
                                disabled={isLoading}
                            >
                                {isLoading ? <><Spinner size="sm" decorative /> Starting…</> : "Start game"}
                            </Button>
                        </div>
                        <div aria-live="polite" className="mt-2">
                            {isLoading && slow && (
                                <p className="gg-text-muted mb-0">Waking up the server. The first game can take up to a minute.</p>
                            )}
                            {!!error && !isLoading && (
                                <Notice className="mb-0">Couldn&apos;t start a game. The server may be down. Try again.</Notice>
                            )}
                        </div>
                    </aside>
                )}

                <div className="home-after">
                <div className="text-center mt-4">
                    <KofiBadge />
                </div>

                <footer className="small gg-text-muted text-center mt-4">
                    Map data: <a href="https://www.naturalearthdata.com/">Natural Earth</a> ·{" "}
                    Names: <a href="https://www.wikidata.org/">Wikidata</a> ·{" "}
                    Tripoints: <a href="https://en.wikipedia.org/wiki/List_of_tripoints">Wikipedia</a>{" "}
                    (<a href="https://creativecommons.org/licenses/by-sa/4.0/">CC BY-SA 4.0</a>) ·{" "}
                    Satellite imagery: <a href="https://www.esri.com/">Esri</a>, Maxar, Earthstar Geographics ·{" "}
                    Icons: <a href="https://www.flaticon.com/free-icons/location-marker" title="location-marker icons">Earthz Stocker</a>,{" "}
                    <a href="https://www.flaticon.com/free-icons/earth-globe" title="earth globe icons">Freepik</a> (Flaticon) ·{" "}
                    <a href="/third-party-licenses.txt">Software licences</a>
                    <br />
                    We record anonymous round results (no cookies, no personal data) to see which questions are hardest.
                    <br />
                    Your browser stores one thing on your device: your light or dark mode choice, if you use the toggle. Nothing else is saved.
                </footer>
                </div>
            </div>
        </div>
    );
}

export default HomePage;
