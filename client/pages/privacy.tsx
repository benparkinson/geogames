import { SITE_NAME } from "../src/siteName";
import Head from "next/head";
import PrivacyPage from "../src/components/PrivacyPage";

function PrivacyRoute(): React.JSX.Element {
  return (
    <>
      <Head>
        <title>{`Credits and privacy | ${SITE_NAME}`}</title>
      </Head>
      <div id="root">
        <PrivacyPage />
      </div>
    </>
  );
}

export default PrivacyRoute;
