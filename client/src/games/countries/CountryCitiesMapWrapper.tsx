import dynamic from "next/dynamic";
import LoadingSpinner from "../../components/LoadingSpinner";

const CountryCitiesMapWrapper = dynamic(() => import("./CountryCitiesMap"), {
  loading: () => <LoadingSpinner />,
  ssr: false,
});

export default CountryCitiesMapWrapper;
