import HomePage from "../src/components/HomePage";
import Head from "next/head";

function MenuPage(): React.JSX.Element {
  return (
    <>
      <Head>
        <title>Geogames</title>
      </Head>
      <div id="root">
        <HomePage />
      </div>
    </>
  );
}

export default MenuPage;
