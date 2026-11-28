import Link from "next/link";
import { useWakeServer } from "../api/health";
import { LANDING, type LandingType } from "../games/landing";
import { MENU_OPTIONS } from "../games/registry";
import GameLinks from "./GameLinks";
import SiteFooter from "./SiteFooter";
import SiteHeader from "./SiteHeader";
import SiteTitle from "./SiteTitle";
import StartPanel from "./StartPanel";

/** One game's own page, for people arriving from a search: what it is, a Start button and how to play. */
export default function LandingPage({ type }: { type: LandingType }): React.JSX.Element {
  const { heading, intro, howToPlay } = LANDING[type];
  // Every landing type is a registered game, which the landing tests check.
  const option = MENU_OPTIONS.find((candidate) => candidate.type === type)!;
  useWakeServer();

  return (
    <div className="gg-page page-full-height page-bg">
      <SiteHeader title={<SiteTitle />} />
      <main className="gg-card gg-page__card gg-page__card--roomy">
        <h1 className="gg-page__heading">{heading}</h1>
        {intro.map((paragraph) => (
          <p key={paragraph}>{paragraph}</p>
        ))}
        <div className="gg-landing__start">
          <StartPanel option={option} howToPlayLink={false} />
        </div>

        <h2 className="gg-page__section">How to play</h2>
        <ol>
          {howToPlay.map((step) => (
            <li key={step}>{step}</li>
          ))}
        </ol>

        <h2 className="gg-page__section">Scoring</h2>
        <p>
          Each round is worth up to 10 points: for the right answer, for taking no hints and for making no wrong
          guesses.
        </p>
        <p>Geoggernaut is free, needs no account and is played in your browser.</p>

        <h2 className="gg-page__section">More games</h2>
        <GameLinks className="gg-landing__games" except={type} />
        <p className="gg-page__last">
          Or take the <Link href="/daily">daily challenge</Link>: five puzzles, the same for everyone today.
        </p>
      </main>
      <SiteFooter />
    </div>
  );
}
