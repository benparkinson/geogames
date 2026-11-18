package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.dao.RoundGuess;
import com.parkinson.benjamin.geogames.helper.PlaceNames;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameType;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * The rules for the guesses, hints and answers of a round. Each method changes the round it is
 * given and throws when the request breaks a rule; it never saves, so {@link GameService} decides
 * when to save and publish. The boolean results say whether the round changed.
 */
@Component
public class RoundRules {

  private static final int MAX_GUESS_LENGTH = 255;

  private final KnownPlaceNames knownPlaceNames;

  public RoundRules(KnownPlaceNames knownPlaceNames) {
    this.knownPlaceNames = knownPlaceNames;
  }

  /**
   * Brings a stored round key up to date when a country in it has been renamed, so games in
   * progress and pinned daily challenges survive a rename.
   */
  String currentRoundKey(String roundKey) {
    return StoredRoundKeys.withCurrentCountryNames(
        roundKey, name -> knownPlaceNames.currentCountryName(name).orElse(name));
  }

  /** @return whether the round changed */
  boolean addHint(GameRoundEntity round, int hint, int guessesBefore) {
    if (round.getAnswerState() != AnswerState.UNANSWERED) {
      throw new RoundClosedException(round.getRoundNumber());
    }
    int taken = round.getHintsTakenAt().size();
    if (hint < taken) {
      // Already recorded: the first request wins, so a retry changes nothing.
      return false;
    }
    if (hint != taken) {
      throw new IllegalArgumentException(
          "Hint %d can't be taken before hint %d".formatted(hint, taken));
    }
    int hintCount = round.getGameType().hintCount();
    if (hint >= hintCount) {
      throw new IllegalArgumentException(
          "A round of %s has %d hints".formatted(round.getGameType(), hintCount));
    }
    if (guessesBefore < 0) {
      throw new IllegalArgumentException("guessesBefore can't be negative");
    }
    // Clamped, not rejected: a guess whose request failed makes the client's count larger than the
    // server's, and that must not turn a real hint into an error.
    round.addHint(Math.min(guessesBefore, round.getGuesses().size()));
    return true;
  }

  /** @return whether the round changed */
  boolean addGuess(GameRoundEntity round, String guess, boolean correct) {
    if (round.getAnswerState() != AnswerState.UNANSWERED) {
      throw new RoundClosedException(round.getRoundNumber());
    }
    if (guess == null || guess.isBlank() || guess.length() > MAX_GUESS_LENGTH) {
      throw new IllegalArgumentException(
          "A guess must be between 1 and %d characters".formatted(MAX_GUESS_LENGTH));
    }
    GameType type = round.getGameType();
    String resolved =
        knownPlaceNames
            .resolve(type.placeKind(), guess)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Not a known %s name".formatted(type.placeKind().name().toLowerCase())));
    String normalised = PlaceNames.normalise(resolved);
    if (round.getGuesses().stream()
        .anyMatch(g -> PlaceNames.normalise(g.getGuess()).equals(normalised))) {
      return false;
    }
    long same = round.getGuesses().stream().filter(g -> g.isCorrect() == correct).count();
    int limit = correct ? type.answerCount() : type.guessLimit();
    if (same >= limit) {
      throw new IllegalArgumentException(
          "A round of %s allows %d %s guesses"
              .formatted(type, limit, correct ? "correct" : "wrong"));
    }
    round.addGuess(new RoundGuess(resolved, correct));
    return true;
  }

  /** Checks the hints and wrong guesses an answer reports against what the round's type allows. */
  void validateAnswer(
      GameRoundEntity round, AnswerState answerState, int hintsUsed, Integer wrongGuesses) {
    validateHintsUsed(round, hintsUsed);
    validateWrongGuesses(round, answerState, wrongGuesses);
  }

  private static void validateHintsUsed(GameRoundEntity round, int hintsUsed) {
    int hintCount = round.getGameType().hintCount();
    if (hintsUsed < 0 || hintsUsed > hintCount) {
      throw new IllegalArgumentException(
          "A round of %s has %d hints, so %d can't have been used"
              .formatted(round.getGameType(), hintCount, hintsUsed));
    }
  }

  /**
   * Checks the reported wrong guesses against the guess limit: running out of guesses means exactly
   * {@code guessLimit} wrong guesses, and any other answer means fewer. A null (not reported) is
   * always accepted.
   */
  private static void validateWrongGuesses(
      GameRoundEntity round, AnswerState answerState, Integer wrongGuesses) {
    if (wrongGuesses == null) {
      return;
    }
    GameType type = round.getGameType();
    int limit = type.guessLimit();
    if (wrongGuesses < 0 || wrongGuesses > limit) {
      throw new IllegalArgumentException(
          "A round of %s allows %d wrong guesses, so %d is not possible"
              .formatted(type, limit, wrongGuesses));
    }
    if (answerState == AnswerState.OUT_OF_GUESSES && wrongGuesses != limit) {
      throw new IllegalArgumentException(
          "A round of %s that ran out of guesses must have %d wrong guesses, not %d"
              .formatted(type, limit, wrongGuesses));
    }
    if (answerState != AnswerState.OUT_OF_GUESSES && wrongGuesses == limit) {
      throw new IllegalArgumentException(
          "A round of %s with %d wrong guesses ran out of guesses, so it can't be %s"
              .formatted(type, wrongGuesses, answerState));
    }
  }

  /**
   * A round's answer is final. Sending the same answer (and hints and wrong guesses) again does
   * nothing, so a retried request is harmless, but changing an answered round is a conflict.
   * Concurrent answers to one round are stopped by the round's version, and the controller retries
   * once.
   *
   * @return whether the round changed
   */
  boolean recordAnswer(
      GameRoundEntity round, AnswerState answerState, int hintsUsed, Integer wrongGuesses) {
    AnswerState current = round.getAnswerState();
    if (current != AnswerState.UNANSWERED) {
      if (current == answerState
          && round.getHintsUsed() == hintsUsed
          && Objects.equals(round.getWrongGuesses(), wrongGuesses)) {
        return false;
      }
      throw new RoundAlreadyAnsweredException(
          round.getRoundNumber(),
          current,
          round.getHintsUsed(),
          round.getWrongGuesses(),
          answerState,
          hintsUsed,
          wrongGuesses);
    }
    round.recordAnswer(answerState, hintsUsed, wrongGuesses);
    return true;
  }
}
