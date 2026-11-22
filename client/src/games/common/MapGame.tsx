import { MapProps } from "../../map/MapProps";
import { AnswerCandidate } from "./answers";
import GameLayout from "./GameLayout";
import GameTopBar from "./GameTopBar";
import type { GuessClue } from "./guessReducer";
import type { HintConfig } from "./hints";
import { Round, RoundAnswer } from "./Model";
import RoundPanel from "./RoundPanel";
import { liveProgress, roundView } from "./roundView";
import type { Suggestion } from "./suggestions";
import { useRoundGuesses } from "./useRoundGuesses";

function MapGame<Type>({
  data,
  answerCount,
  MapComponent,
  answers,
  round,
  prompt,
  inputPlaceholder,
  answerNoun,
  blankMap,
  suggestions,
  isKnownGuess,
  hints,
  clueFor,
  submitAnswer,
}: MapGameProps<Type>): React.JSX.Element {
  const { state, submitGuess, giveUp, takeHint } = useRoundGuesses({
    round,
    answers,
    answerCount,
    isKnownGuess,
    clueFor,
    submitAnswer,
  });
  const { gaveUp, over: gameOver, hintsUsed } = state;
  const view = roundView(round, state, answerCount, hints);

  const map = (
    <MapComponent
      data={data}
      gameOver={gameOver}
      gaveUp={gaveUp}
      hintsUsed={hintsUsed}
      foundAtHint={state.foundAtHint}
    />
  );

  const panel = (
    <RoundPanel
      round={round}
      state={state}
      view={view}
      prompt={prompt}
      inputPlaceholder={inputPlaceholder}
      answerNoun={answerNoun}
      suggestions={suggestions}
      allAnswers={answers.map((a) => a.name)}
      hints={hints}
      submitGuess={submitGuess}
      giveUp={giveUp}
      takeHint={takeHint}
    />
  );

  return (
    <GameLayout
      topBar={<GameTopBar round={round} progress={liveProgress(round, view)} />}
      map={map}
      panel={panel}
      mapClassName={blankMap && !gameOver ? "game-map--blank" : undefined}
    />
  );
}

export interface MapGameProps<Type> {
  data: Type;
  answerCount: number;
  MapComponent: React.ComponentType<MapProps<Type>>;
  /** The answers to find, with the aliases that also count. */
  answers: AnswerCandidate[];
  round: Round;
  prompt: string;
  inputPlaceholder: string;
  /** What the player is naming, singular: used in "We don't recognise "x" as a country". */
  answerNoun: string;
  /** Shows a grey map instead of the real one while the round is being played. */
  blankMap?: boolean;
  suggestions?: Suggestion[];
  isKnownGuess?: (guess: string) => boolean;
  /** What the round's hints say. The round's `hintCount` (from the backend) says how many there are. */
  hints: HintConfig;
  /**
   * Gives a wrong guess a clue (distance and direction to the nearest answer not found yet). Leave it
   * out for a game whose map already shows where the answer is. Hard mode hides the clues.
   */
  clueFor?: (guess: string, targets: string[]) => GuessClue | null;
  submitAnswer: (answer: RoundAnswer) => void;
}

export default MapGame;
