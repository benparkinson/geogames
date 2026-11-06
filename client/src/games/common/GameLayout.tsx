import { ReactNode } from "react";

export default function GameLayout({ topBar, map, panel, mapClassName }: { topBar: ReactNode; map: ReactNode; panel: ReactNode; mapClassName?: string }) {
  return (
    <div className="game-screen">
      <header className="game-top">{topBar}</header>
      <main className={"game-map" + (mapClassName ? " " + mapClassName : "")}>{map}</main>
      <aside className="game-panel">{panel}</aside>
    </div>
  );
}
