import Head from "next/head";
import { useRouter } from "next/router";
import ChallengePage from "../src/challenge/ChallengePage";

function ChallengeRoute(): React.JSX.Element {
  const router = useRouter();
  const { id } = router.query;
  return (
    <>
      <Head>
        <title>Challenge | Geoggernaut</title>
      </Head>
      <div id="root">
        <ChallengePage id={router.isReady ? (typeof id === "string" ? id : "") : null} />
      </div>
    </>
  );
}

export default ChallengeRoute;
