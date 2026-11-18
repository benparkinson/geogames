import Head from "next/head";
import DailyPage from "../src/daily/DailyPage";

function DailyRoute(): React.JSX.Element {
  return (
    <>
      <Head>
        <title>Daily challenge | GeoGauntlet</title>
      </Head>
      <div id="root">
        <DailyPage />
      </div>
    </>
  );
}

export default DailyRoute;
