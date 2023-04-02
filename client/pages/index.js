import Link from "next/link";
import Head from "next/head";

function MenuPage() {
  return (
    <div>
      <Head>
        <title>Geogames!</title>
      </Head>
      <Link href="/tripoint">Tripoint</Link>
    </div>
  );
}

export default MenuPage;
