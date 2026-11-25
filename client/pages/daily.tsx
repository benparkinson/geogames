import { SITE_NAME } from "../src/siteName";
import Head from "next/head";
import DailyPage from "../src/daily/DailyPage";

function DailyRoute(): React.JSX.Element {
  return (
    <>
      <Head>
        <title>{`Daily challenge | ${SITE_NAME}`}</title>
      </Head>
      <div id="root">
        <DailyPage />
      </div>
    </>
  );
}

export default DailyRoute;
