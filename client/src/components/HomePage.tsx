import { useState } from "react";
import { useWakeServer } from "../api/health";
import { MENU_OPTIONS } from "../games/registry";
import DailyCard from "./DailyCard";
import GameCardGrid from "./GameCardGrid";
import KofiBadge from "./KofiBadge";
import SiteFooter from "./SiteFooter";
import StartPanel from "./StartPanel";
import ThemeToggle from "./ThemeToggle";

function HomePage(): React.JSX.Element {
  const [selected, setSelected] = useState<string | null>(null);
  const selectedOption = MENU_OPTIONS.find((o) => o.type === selected);
  useWakeServer();

  return (
    <div className={"min-vh-100 py-4 page-bg" + (selectedOption ? " home--selected" : "")}>
      <header className="home-header">
        <div className="d-flex justify-content-end">
          <ThemeToggle />
        </div>
        <h1 className="text-center mt-3">GeoGauntlet</h1>
        <p className="lead text-center mb-4">Quick but challenging geography puzzles.</p>
      </header>
      <div className="home-layout">
        <section className="home-main">
          <DailyCard />
          <h2 className="h5">Choose a game</h2>
          <GameCardGrid selected={selected} onSelect={setSelected} />
        </section>

        {selectedOption && <StartPanel option={selectedOption} />}

        <div className="home-after">
          <div className="text-center mt-4">
            <KofiBadge />
          </div>

          <SiteFooter />
        </div>
      </div>
    </div>
  );
}

export default HomePage;
