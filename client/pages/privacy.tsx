import Head from "next/head";
import PrivacyPage from "../src/components/PrivacyPage";

function PrivacyRoute(): React.JSX.Element {
  return (
    <>
      <Head>
        <title>Credits and privacy | GeoGauntlet</title>
      </Head>
      <div id="root">
        <PrivacyPage />
      </div>
    </>
  );
}

export default PrivacyRoute;
