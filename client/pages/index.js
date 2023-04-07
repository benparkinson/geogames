import Link from "next/link";
import Head from "next/head";
import { Button } from "../src/components/Button";
import isProduction from "../src/config/environment";

function MenuPage() {
  function createLink(pageName) {
    if (isProduction()) {
      return "/" + pageName + ".html";
    } else {
      return "/" + pageName;
    }
  }

  return (
    <div>
      <Head>
        <title>Geogames!</title>
      </Head>
      <div className="d-flex vh-100">
        <div className="container d-flex justify-content-center align-items-center text-center">
          <div className="row d-flex flex-column">
            <h4>Welcome to Geogames (working title)</h4>
            <h6>Games:</h6>
            <div className="m-1">
              <Link href={createLink("tripoint")}>
                <Button text={"Tripoint"} bootstrapClass={"btn-info"}></Button>
              </Link>
            </div>
            <div className="m-1">
              <Link href={createLink("rivershapes")}>
                <Button text={"Rivers by Shape"} bootstrapClass={"btn-info"}></Button>
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default MenuPage;
