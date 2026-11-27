import Head from "next/head";
import { useRouter } from "next/router";
import ChallengePage from "../src/challenge/ChallengePage";
import { SocialMeta } from "../src/components/SocialMeta";
import { SITE_NAME } from "../src/siteName";

function ChallengeRoute(): React.JSX.Element {
  const router = useRouter();
  const { id } = router.query;
  return (
    <>
      <Head>
        <title>{`Challenge | ${SITE_NAME}`}</title>
      </Head>
      <SocialMeta page="challenge" />
      <div id="root">
        <ChallengePage id={router.isReady ? (typeof id === "string" ? id : "") : null} />
      </div>
    </>
  );
}

export default ChallengeRoute;
