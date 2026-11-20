import Link from "next/link";

/** The credits and privacy notes shown at the foot of every page that isn't a full-screen game. */
function SiteFooter({ className = "mt-4" }: { className?: string }): React.JSX.Element {
  return (
    <footer className={"small gg-text-muted text-center " + className}>
      Maps and relief:<a href="https://www.naturalearthdata.com/">Natural Earth</a> · Names:{" "}
      <a href="https://www.wikidata.org/">Wikidata</a> · Tripoints:{" "}
      <a href="https://en.wikipedia.org/wiki/List_of_tripoints">Wikipedia</a> (
      <a href="https://creativecommons.org/licenses/by-sa/4.0/">CC BY-SA 4.0</a>, positions adjusted) · Icons:{" "}
      <a href="https://www.flaticon.com/free-icons/location-marker" title="location-marker icons">
        Earthz Stocker
      </a>
      ,{" "}
      <a href="https://www.flaticon.com/free-icons/earth-globe" title="earth globe icons">
        Freepik
      </a>{" "}
      (Flaticon) · <a href="/third-party-licenses.txt">Software licences</a>
      <br />
      We record anonymous round results (no cookies, no personal data) to see which questions are hardest.
      <br />
      Your browser keeps a few settings on your device, only if you use them: your light or dark mode choice, your
      difficulty and today&apos;s daily challenge. Nothing else is saved.
      <br />
      <Link href="/privacy">Privacy</Link>
    </footer>
  );
}

export default SiteFooter;
