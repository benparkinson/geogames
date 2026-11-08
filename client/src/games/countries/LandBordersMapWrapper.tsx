import dynamic from "next/dynamic";
import LoadingSpinner from "../../components/LoadingSpinner";

const LandBordersMapWrapper = dynamic(() => import("./LandBordersMap"), {
  loading: () => <LoadingSpinner />,
  ssr: false,
});

export default LandBordersMapWrapper;
