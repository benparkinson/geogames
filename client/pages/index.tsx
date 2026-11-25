import { SITE_NAME } from "../src/siteName";
import HomePage from "../src/components/HomePage";
import Head from "next/head";

function MenuPage(): React.JSX.Element {
  return (
    <>
      <Head>
        <title>{SITE_NAME}</title>
      </Head>
      <div id="root">
        <HomePage />
      </div>
    </>
  );
}

export default MenuPage;
