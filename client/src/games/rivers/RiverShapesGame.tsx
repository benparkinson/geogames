import MapGame from "../common/MapGame";
import type { GameComponentProps } from "../types";
import RiverShapesMapWrapper from "./RiverShapesMapWrapper";
import type { River } from "./model";

export default function RiverShapesGame({ data: river, round, answerCount, submitAnswer }: GameComponentProps<River>): React.JSX.Element {
  return (
    <MapGame
      data={river}
      answerCount={answerCount}
      MapComponent={RiverShapesMapWrapper}
      answers={[river]}
      round={round}
      blankMapCaption="Shape only. The satellite map appears when you answer."
      prompt="Name this river from its shape."
      inputPlaceholder="River name…"
      answerNoun="river"
      hints={{
        total: 1,
        description: "Shows which continent the river is in",
        text: () => `Continent: ${river.continent}`
      }}
      submitAnswer={submitAnswer}
    />
  );
}
