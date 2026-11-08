import dynamic from "next/dynamic";
import LoadingSpinner from "../../components/LoadingSpinner";

const NeighboursMapWrapper = dynamic(() => import("./NeighboursMap"), {
  loading: () => <LoadingSpinner />,
  ssr: false,
});

export default NeighboursMapWrapper;
