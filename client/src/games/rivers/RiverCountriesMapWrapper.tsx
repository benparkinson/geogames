import dynamic from "next/dynamic";
import LoadingSpinner from "../../components/LoadingSpinner";

const RiverCountriesMapWrapper = dynamic(() => import("./RiverCountriesMap"), {
  loading: () => <LoadingSpinner />,
  ssr: false,
});

export default RiverCountriesMapWrapper;
