import { useRouter } from "next/router";
import ChallengePage from "../src/challenge/ChallengePage";
import { PageMeta } from "../src/components/PageMeta";

function ChallengeRoute(): React.JSX.Element {
  const router = useRouter();
  const { id } = router.query;
  return (
    <>
      <PageMeta page="challenge" />
      <div id="root">
        <ChallengePage id={router.isReady ? (typeof id === "string" ? id : "") : null} />
      </div>
    </>
  );
}

export default ChallengeRoute;
