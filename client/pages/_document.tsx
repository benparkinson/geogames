import { Html, Head, Main, NextScript } from "next/document";

// A theme the visitor chose with the toggle (kept in localStorage) wins; otherwise follow the system setting.
const themeScript = `
(function () {
  var query = window.matchMedia("(prefers-color-scheme: dark)");
  function apply() {
    var stored = null;
    try { stored = localStorage.getItem("gg-theme"); } catch (e) {}
    var theme = stored === "dark" || stored === "light" ? stored : query.matches ? "dark" : "light";
    document.documentElement.setAttribute("data-bs-theme", theme);
  }
  apply();
  query.addEventListener("change", apply);
})();
`;

export default function Document(): React.JSX.Element {
  return (
    <Html lang="en">
      <Head>
        <script dangerouslySetInnerHTML={{ __html: themeScript }} />
      </Head>
      <body>
        <Main />
        <NextScript />
      </body>
    </Html>
  );
}
