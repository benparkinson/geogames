import dynamic from "next/dynamic";
import LoadingSpinner from "../../components/LoadingSpinner";

const MergedCountriesMapWrapper = dynamic(() => import("./MergedCountriesMap"), {
  loading: () => <LoadingSpinner />,
  ssr: false,
});

export default MergedCountriesMapWrapper;
