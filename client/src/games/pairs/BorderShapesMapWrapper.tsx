import dynamic from "next/dynamic";
import LoadingSpinner from "../../components/LoadingSpinner";

const BorderShapesMapWrapper = dynamic(() => import("./BorderShapesMap"), {
  loading: () => <LoadingSpinner />,
  ssr: false,
});

export default BorderShapesMapWrapper;
