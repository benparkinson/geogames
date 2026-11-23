import SiteTitle from "./SiteTitle";
import SiteHeader from "./SiteHeader";

// TODO: replace with the real contact address before the site goes live.
export const OWNER_NAME = "Ben Parkinson";
export const CONTACT_EMAIL = "privacy@example.com";

function PrivacyPage(): React.JSX.Element {
  return (
    <div className="gg-page page-full-height page-bg">
      <SiteHeader title={<SiteTitle />} />
      <main className="gg-card gg-page__card gg-page__card--roomy">
        <h1 className="gg-page__heading">Credits and privacy</h1>

        <h2 className="gg-page__section">Credits</h2>
        <p>
          Maps and relief: <a href="https://www.naturalearthdata.com/">Natural Earth</a> (public domain). Names:{" "}
          <a href="https://www.wikidata.org/">Wikidata</a>. Tripoints:{" "}
          <a href="https://en.wikipedia.org/wiki/List_of_tripoints">Wikipedia</a> (
          <a href="https://creativecommons.org/licenses/by-sa/4.0/">CC BY-SA 4.0</a>, positions adjusted). Fonts:{" "}
          <a href="https://fontsource.org/fonts/archivo">Archivo</a> and{" "}
          <a href="https://fontsource.org/fonts/ibm-plex-mono">IBM Plex Mono</a> (
          <a href="https://openfontlicense.org/">SIL Open Font Licence 1.1</a>), served from this site.{" "}
          <a href="/third-party-licenses.txt">Software licences</a>.
        </p>

        <h2 className="gg-page__section">Who runs this site</h2>
        <p>
          {OWNER_NAME} runs GeoGauntlet. For privacy questions, write to{" "}
          <a href={"mailto:" + CONTACT_EMAIL}>{CONTACT_EMAIL}</a>.
        </p>

        <h2 className="gg-page__section">What we store</h2>
        <p>
          There are no accounts, no cookies, no analytics and no advertising. A game is a random id plus the questions
          asked, the guesses made (only names from our lists), the hints taken, the difficulty (Easy, Normal or Hard),
          how each round ended and how many of its answers you found. It is deleted after 24 hours. To stop abuse, when
          you start a game the server keeps a scrambled form of your network address in memory for up to ten minutes. It
          is never stored on disk or logged.
        </p>

        <h2 className="gg-page__section">Anonymous statistics</h2>
        <p>
          For each answered round we keep the date, the game type, the question, the result, and the hints and wrong
          guesses used, and the difficulty. Nothing in it identifies a person or a device, so it is kept indefinitely.
        </p>

        <h2 className="gg-page__section">On your device</h2>
        <p>
          Your browser keeps up to four items in its local storage: <code>gg-theme</code>, <code>gg-difficulty</code>{" "}
          (your difficulty setting), <code>gg-daily</code> and <code>gg-daily-streak</code>. The first two are set only
          if you use their control (the Appearance and difficulty settings; System default removes the first). The third
          is set only when you play the daily challenge: it holds today&apos;s game id, its difficulty and, once you
          finish, your score and how each round ended. It is replaced the next day and never sent anywhere. The fourth
          holds the date you last finished the daily challenge and how many days in a row you have finished it. It is
          set only when you finish one and is never sent anywhere. Clearing site data removes all four.
        </p>
        <p>
          Everyone who plays the daily challenge on a date gets the same rounds. The server stores which rounds those
          are for each date, and nothing about who played them. When you finish the daily challenge, your score is added
          to that day&apos;s anonymous totals for your difficulty (Easy, Normal or Hard): the number of players, the sum
          of their scores, and how many scored in each tenth of the range. Nothing in the totals links back to a game or
          a player, and they are kept indefinitely. Copying your result writes to your clipboard only when you click
          Copy result.
        </p>

        <h2 className="gg-page__section">Who else receives data</h2>
        <p>
          Loading any web page sends your IP address to the server that hosts it.{" "}
          <a href="https://render.com/privacy">Render</a> (render.com, a United States company) hosts this site, its map
          images and its game server, so it receives yours.
        </p>
        <p>
          While the site is open it occasionally fetches a small file from the same server to check whether a newer
          version has been published. The request contains nothing about you.
        </p>
        <p>The &quot;Buy me a coffee&quot; link opens Ko-fi. Nothing is sent to Ko-fi unless you click it.</p>

        <h2 className="gg-page__section">Why this is allowed</h2>
        <p>The legal basis is legitimate interests: the site cannot be delivered without these requests.</p>

        <h2 className="gg-page__section">Your rights</h2>
        <p>
          You can ask what data we hold about you and ask for it to be deleted, using the contact above. We hold nothing
          that identifies you, so there will usually be nothing to find. You can also complain to the data protection
          authority of your German federal state. The{" "}
          <a href="https://www.bfdi.bund.de/DE/Service/Anschriften/Laender/Laender-node.html">
            list of German data protection authorities
          </a>{" "}
          is on the website of the federal commissioner (BfDI).
        </p>

        <h2 className="gg-page__section">Changes</h2>
        <p className="gg-page__last">Last updated: 18 November 2026.</p>
      </main>
    </div>
  );
}

export default PrivacyPage;
