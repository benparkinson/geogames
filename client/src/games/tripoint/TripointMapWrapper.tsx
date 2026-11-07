import dynamic from "next/dynamic";
import LoadingSpinner from "../../components/LoadingSpinner";

const TripointMapWrapper = dynamic(() => import("./TripointMap"), {
  loading: () => <LoadingSpinner />,
  ssr: false,
});

export default TripointMapWrapper;
