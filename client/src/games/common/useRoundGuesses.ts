import { useEffect, useEffectEvent, useRef, useState } from "react";
import { AnswerCandidate } from "./answers";
import {
  GuessAction,
  GuessConfig,
  foundAnswers,
  GuessResult,
  guessReducer,
  GuessState,
  restoreGuessState,
  withLateClues,
  wrongGuessCount,
} from "./guessReducer";
import { CORRECT_ANSWER, Round, RoundAnswer, UNANSWERED } from "./Model";

/**
 * The state of one round's guesses and hints, and what the player can do to it. It is restored from what the server
 * saved, saves each guess and hint as it is made, and reports the answer when the round ends.
 */
export function useRoundGuesses({
  round,
  answers,
  answerCount,
  isKnownGuess,
  namesLoading = false,
  clueFor,
  submitAnswer,
}: {
  round: Round;
  answers: AnswerCandidate[];
  answerCount: number;
  isKnownGuess?: (guess: string) => boolean;
  /** The name list is still loading: a guess is not taken, as it could not be checked. */
  namesLoading?: boolean;
  clueFor?: GuessConfig["clueFor"];
  submitAnswer: (answer: RoundAnswer) => void;
}): {
  state: GuessState;
  submitGuess: (text: string) => GuessResult;
  giveUp: () => void;
  takeHint: () => void;
} {
  /** All the answers for a correct round, otherwise the ones found so far. */
  const answersFoundAt = (finished: RoundAnswer["answerState"], at: GuessState) =>
    finished === CORRECT_ANSWER ? answerCount : foundAnswers(at).length;
  const config: GuessConfig = {
    answers,
    answerCount,
    isKnownGuess,
    hintsTotal: round.hintCount,
    guessLimit: round.guessLimit,
    clueFor,
  };
  // Rebuilt from the guesses and hints the server saved, so a refresh keeps them.
  const [restored] = useState(() =>
    restoreGuessState(round.answerState, round.hintsUsed, round.guesses, round.hintsTakenAt, config),
  );
  const [savedState, setSavedState] = useState<GuessState>(restored.state);
  // The country list that the clues come from may load after the round was restored. Clues are computed, never
  // saved, so once it has loaded, work out the ones the restored guesses missed (a refresh must show them too).
  const state = clueFor
    ? withLateClues(
        savedState,
        () =>
          restoreGuessState(round.answerState, round.hintsUsed, round.guesses, round.hintsTakenAt, config).state
            .guesses,
      )
    : savedState;

  // The guesses ended the round but the answer request after the last guess was lost: send it again, once.
  const resubmitted = useRef(false);
  const resubmit = useEffectEvent(() => {
    if (restored.finished && round.answerState === UNANSWERED && !resubmitted.current) {
      resubmitted.current = true;
      submitAnswer({
        answerState: restored.finished,
        hintsUsed: restored.state.hintsUsed,
        wrongGuesses: wrongGuessCount(restored.state),
        answersFound: answersFoundAt(restored.finished, restored.state),
      });
    }
  });
  // Once, for the state the round was opened with.
  useEffect(() => {
    resubmit();
  }, []);

  function dispatch(action: GuessAction) {
    const outcome = guessReducer(state, action, config);
    setSavedState(outcome.state);
    // Saved before the answer: the two share a mutation scope, which runs them in the order they were started.
    if (outcome.state.guesses.length > state.guesses.length) {
      const last = outcome.state.guesses[outcome.state.guesses.length - 1];
      round.recordGuess({ text: last.text, correct: last.answer !== null });
    }
    if (outcome.finished) {
      submitAnswer({
        answerState: outcome.finished,
        hintsUsed: outcome.state.hintsUsed,
        wrongGuesses: wrongGuessCount(outcome.state),
        answersFound: answersFoundAt(outcome.finished, outcome.state),
      });
    }
    return outcome;
  }

  function submitGuess(text: string): GuessResult {
    if (namesLoading && !state.over && text.trim() !== "") {
      return { kind: "loading" };
    }
    return dispatch({ type: "guess", text }).result ?? { kind: "over" };
  }

  function giveUp() {
    dispatch({ type: "giveUp" });
  }

  function takeHint() {
    if (state.over || state.hintsUsed >= round.hintCount) {
      return;
    }
    // The state before the hint: the index of the hint just taken and the guesses made so far.
    round.recordHint({ hint: state.hintsUsed, guessesBefore: state.guesses.length });
    dispatch({ type: "hint" });
  }

  return { state, submitGuess, giveUp, takeHint };
}
