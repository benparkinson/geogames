import DailyPage from "../src/daily/DailyPage";
import { PageMeta } from "../src/components/PageMeta";

function DailyRoute(): React.JSX.Element {
  return (
    <>
      <PageMeta page="daily" />
      <div id="root">
        <DailyPage />
      </div>
    </>
  );
}

export default DailyRoute;
