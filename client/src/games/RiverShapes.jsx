import RiverShapesMapWrapper from "../map/RiverShapesMapWrapper";
import MapGame from "./common/MapGame";

function RiverShapes() {
  function guessBoxName() {
    return "River name";
  }

  function correctAnswers(river) {
    return [river.properties.name];
  }

  function checkAdditionalAnswers(guess, river) {
    return guess;
  }

  return (
    <MapGame
      dataName={"River"}
      serverRoute={"/api/river-shape"}
      guessBoxCount={1}
      guessBoxName={guessBoxName}
      MapComponent={RiverShapesMapWrapper}
      correctAnswersFunction={correctAnswers}
      checkAdditionalAnswers={checkAdditionalAnswers}
    />
  );
}

export default RiverShapes;
