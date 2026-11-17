import HomeLink from "./HomeLink";
import SiteFooter from "./SiteFooter";
import ThemeToggle from "./ThemeToggle";

// TODO: replace with the real contact address before the site goes live.
export const OWNER_NAME = "Ben Parkinson";
export const CONTACT_EMAIL = "privacy@example.com";

function PrivacyPage(): React.JSX.Element {
    return (
        <div className="min-vh-100 page-bg">
            <div className="px-3 py-2 d-flex justify-content-between align-items-center">
                <div className="d-flex align-items-center gap-2">
                    <HomeLink />
                    <ThemeToggle />
                </div>
            </div>
            <main className="gg-card mx-auto my-4 p-3">
                <h1 className="h3 mb-3">Privacy</h1>

                <h2 className="h5">Who runs this site</h2>
                <p>
                    {OWNER_NAME} runs GeoGauntlet. For privacy questions, write to{" "}
                    <a href={"mailto:" + CONTACT_EMAIL}>{CONTACT_EMAIL}</a>.
                </p>

                <h2 className="h5">What we store</h2>
                <p>
                    There are no accounts, no cookies, no analytics and no advertising. A game is a random id plus the
                    questions asked and how each round ended. It is deleted after 24 hours.
                </p>

                <h2 className="h5">Anonymous statistics</h2>
                <p>
                    For each answered round we keep the date, the game type, the question, the result, and the hints and
                    wrong guesses used. Nothing in it identifies a person or a device, so it is kept indefinitely.
                </p>

                <h2 className="h5">On your device</h2>
                <p>
                    Your browser keeps up to two items in its local storage: <code>gg-theme</code> and{" "}
                    <code>gg-hardcore</code>. Each is set only if you use its switch (the light/dark toggle and
                    Hardcore mode). Clearing site data removes them.
                </p>

                <h2 className="h5">Who else receives data</h2>
                <p>Loading any web page sends your IP address to the server that hosts it. Two companies receive yours:</p>
                <ul>
                    <li>
                        <a href="https://render.com/privacy">Render</a> (render.com, a United States company) hosts this
                        site and its game server.
                    </li>
                    <li>
                        <a href="https://cloudless.eox.at">EOX IT Services GmbH</a> (Austria) serves the satellite
                        images. Your browser asks EOX for them directly whenever a satellite map is shown.
                    </li>
                </ul>
                <p>The &quot;Buy me a coffee&quot; link opens Ko-fi. Nothing is sent to Ko-fi unless you click it.</p>

                <h2 className="h5">Why this is allowed</h2>
                <p>
                    The legal basis is legitimate interests: the site cannot be delivered without these requests.
                </p>

                <h2 className="h5">Your rights</h2>
                <p>
                    You can ask what data we hold about you and ask for it to be deleted, using the contact above. We
                    hold nothing that identifies you, so there will usually be nothing to find. You can also complain to
                    the data protection authority of your German federal state. The{" "}
                    <a href="https://www.bfdi.bund.de/DE/Service/Anschriften/Laender/Laender-node.html">
                        list of German data protection authorities
                    </a>{" "}
                    is on the website of the federal commissioner (BfDI).
                </p>

                <h2 className="h5">Changes</h2>
                <p className="mb-0">Last updated: 17 November 2026.</p>
            </main>
            <SiteFooter className="pb-4" />
        </div>
    );
}

export default PrivacyPage;
