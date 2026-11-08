import dynamic from "next/dynamic";
import LoadingSpinner from "../../components/LoadingSpinner";

const CoastlineMapWrapper = dynamic(() => import("./CoastlineMap"), {
  loading: () => <LoadingSpinner />,
  ssr: false,
});

export default CoastlineMapWrapper;
