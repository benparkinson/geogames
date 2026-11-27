import HomePage from "../src/components/HomePage";
import { PageMeta } from "../src/components/PageMeta";

function MenuPage(): React.JSX.Element {
  return (
    <>
      <PageMeta page="site" />
      <div id="root">
        <HomePage />
      </div>
    </>
  );
}

export default MenuPage;
