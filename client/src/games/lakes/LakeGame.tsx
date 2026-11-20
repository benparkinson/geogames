import PlaceGame from "../common/PlaceGame";
import type { GameComponentProps } from "../types";
import { lakeHints } from "./lakeHints";
import LakeShapesMapWrapper from "./LakeShapesMapWrapper";
import type { LakeRound } from "./model";

export default function LakeGame(game: GameComponentProps<LakeRound>): React.JSX.Element {
  return (
    <PlaceGame
      kind="lakes"
      game={game}
      MapComponent={LakeShapesMapWrapper}
      answers={[game.data.lake]}
      blankMap
      prompt="Which lake is this?"
      hints={lakeHints(game.data.lake, game.round.continents.length > 0)}
    />
  );
}
