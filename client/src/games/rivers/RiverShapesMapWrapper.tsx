import dynamic from "next/dynamic";
import LoadingSpinner from "../../components/LoadingSpinner";

const RiverShapesMapWrapper = dynamic(() => import("./RiverShapesMap"), {
  loading: () => <LoadingSpinner />,
  ssr: false,
});

export default RiverShapesMapWrapper;
