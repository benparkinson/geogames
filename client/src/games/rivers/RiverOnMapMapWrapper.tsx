import dynamic from "next/dynamic";
import LoadingSpinner from "../../components/LoadingSpinner";

const RiverOnMapMapWrapper = dynamic(() => import("./RiverOnMapMap"), {
  loading: () => <LoadingSpinner />,
  ssr: false,
});

export default RiverOnMapMapWrapper;
