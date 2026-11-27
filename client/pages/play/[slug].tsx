import type { GetStaticPaths, GetStaticProps } from "next";
import LandingPage from "../../src/components/LandingPage";
import { PageMeta } from "../../src/components/PageMeta";
import { LANDING, LANDING_TYPES, type LandingType } from "../../src/games/landing";

interface Props {
  type: LandingType;
}

export const getStaticPaths: GetStaticPaths<{ slug: string }> = () => ({
  paths: LANDING_TYPES.map((type) => ({ params: { slug: LANDING[type].slug } })),
  fallback: false,
});

export const getStaticProps: GetStaticProps<Props, { slug: string }> = ({ params }) => {
  const type = LANDING_TYPES.find((candidate) => LANDING[candidate].slug === params?.slug);
  return type ? { props: { type } } : { notFound: true };
};

function LandingRoute({ type }: Props): React.JSX.Element {
  return (
    <>
      <PageMeta page={type} />
      <div id="root">
        {/* Keyed so that moving between games starts each page afresh, with its own round count. */}
        <LandingPage key={type} type={type} />
      </div>
    </>
  );
}

export default LandingRoute;
