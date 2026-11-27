import { PageMeta } from "../src/components/PageMeta";
import PrivacyPage from "../src/components/PrivacyPage";

function PrivacyRoute(): React.JSX.Element {
  return (
    <>
      <PageMeta page="privacy" />
      <div id="root">
        <PrivacyPage />
      </div>
    </>
  );
}

export default PrivacyRoute;
