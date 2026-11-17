import Head from "next/head";
import PrivacyPage from "../src/components/PrivacyPage";

function PrivacyRoute(): React.JSX.Element {
  return (
    <>
      <Head>
        <title>Privacy | GeoGauntlet</title>
      </Head>
      <div id="root">
        <PrivacyPage />
      </div>
    </>
  );
}

export default PrivacyRoute;
