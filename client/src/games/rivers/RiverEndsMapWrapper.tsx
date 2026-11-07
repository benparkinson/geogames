import dynamic from "next/dynamic";
import LoadingSpinner from "../../components/LoadingSpinner";

const RiverEndsMapWrapper = dynamic(() => import("./RiverEndsMap"), {
  loading: () => <LoadingSpinner />,
  ssr: false,
});

export default RiverEndsMapWrapper;
