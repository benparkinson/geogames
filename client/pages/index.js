import Link from "next/link";
import Head from "next/head";
import { Button } from "../src/components/Button";

function MenuPage() {
  return (
    <div>
      <Head>
        <title>Geogames!</title>
      </Head>
      <div className="d-flex vh-100">
        <div className="container d-flex justify-content-center align-items-center text-center">
          <div className="row">
            <div className="col">
              <h4>Welcome to Geogames (working title)</h4>
              <h6>Games:</h6>
              <Link href="/tripoint.html">
                <Button text={"Tripoint"} bootstrapClass={"btn-info"}></Button>
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default MenuPage;
