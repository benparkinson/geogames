import { useState } from "react";
import { useWakeServer } from "../api/health";
import DailyCard from "./DailyCard";
import GameCardGrid from "./GameCardGrid";
import KofiBadge from "./KofiBadge";
import SiteFooter from "./SiteFooter";
import SiteHeader from "./SiteHeader";
import StartPanel from "./StartPanel";

function HomePage(): React.JSX.Element {
  const [selected, setSelected] = useState<string | null>(null);
  useWakeServer();

  return (
    <div className="home page-bg">
      <SiteHeader title={<h1 className="gg-site-header__title">GeoGauntlet</h1>} />
      <div className="home-layout">
        <section className="home-daily">
          <DailyCard />
        </section>
        <section className="home-main">
          <GameCardGrid
            selected={selected}
            onSelect={setSelected}
            renderPanel={(option) => <StartPanel option={option} />}
          />
        </section>

        <div className="home-after">
          <KofiBadge />
          <SiteFooter />
        </div>
      </div>
    </div>
  );
}

export default HomePage;
