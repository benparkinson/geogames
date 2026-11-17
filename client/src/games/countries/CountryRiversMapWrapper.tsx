import dynamic from "next/dynamic";
import LoadingSpinner from "../../components/LoadingSpinner";

const CountryRiversMapWrapper = dynamic(() => import("./CountryRiversMap"), {
  loading: () => <LoadingSpinner />,
  ssr: false,
});

export default CountryRiversMapWrapper;
