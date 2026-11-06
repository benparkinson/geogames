import { Html, Head, Main, NextScript } from "next/document";

const themeScript = `
(function () {
  var query = window.matchMedia("(prefers-color-scheme: dark)");
  function apply() {
    document.documentElement.setAttribute("data-bs-theme", query.matches ? "dark" : "light");
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
