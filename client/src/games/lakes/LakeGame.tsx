import MapGame from "../common/MapGame";
import { useGuessList } from "../common/useGuessList";
import type { GameComponentProps } from "../types";
import { lakeHints } from "./lakeHints";
import LakeShapesMapWrapper from "./LakeShapesMapWrapper";
import type { LakeRound } from "./model";

export default function LakeGame({ data, round, answerCount, submitAnswer }: GameComponentProps<LakeRound>): React.JSX.Element {
  const { suggestions, isKnownGuess } = useGuessList("lakes");
  return (
    <MapGame
      data={data}
      answerCount={answerCount}
      MapComponent={LakeShapesMapWrapper}
      answers={[data.lake]}
      round={round}
      suggestions={suggestions}
      isKnownGuess={isKnownGuess}
      blankMap
      mapCaption="Shape only. The scale bar shows its size."
      prompt="Name this lake from its shape."
      inputPlaceholder="Lake name…"
      answerNoun="lake"
      hints={lakeHints(data.lake)}
      submitAnswer={submitAnswer}
    />
  );
}
