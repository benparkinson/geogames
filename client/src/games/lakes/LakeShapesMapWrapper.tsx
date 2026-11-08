import dynamic from "next/dynamic";
import LoadingSpinner from "../../components/LoadingSpinner";

const LakeShapesMapWrapper = dynamic(() => import("./LakeShapesMap"), {
  loading: () => <LoadingSpinner />,
  ssr: false,
});

export default LakeShapesMapWrapper;
