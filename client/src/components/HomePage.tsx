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
    <div className={"home page-bg" + (selectedOption ? " home--selected" : "")}>
      <header className="home-header">
        <div className="home-header__toolbar">
          <ThemeToggle />
        </div>
        <h1 className="home-header__title">GeoGauntlet</h1>
        <p className="home-header__tagline">Quick but challenging geography puzzles.</p>
      </header>
      <div className="home-layout">
        <section className="home-main">
          <DailyCard />
          <h2 className="home-main__heading">Practice mode</h2>
          <p className="home-main__note gg-text-muted">Pick any game to practise.</p>
          <GameCardGrid selected={selected} onSelect={setSelected} />
        </section>

        {selectedOption && <StartPanel option={selectedOption} />}

        <div className="home-after">
          <div className="home-after__kofi">
            <KofiBadge />
          </div>

          <SiteFooter />
        </div>
      </div>
    </div>
  );
}

export default HomePage;
