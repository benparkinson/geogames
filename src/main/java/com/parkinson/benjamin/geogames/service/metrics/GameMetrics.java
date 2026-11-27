package com.parkinson.benjamin.geogames.service.metrics;

import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameOrigin;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.stats.GameFinished;
import com.parkinson.benjamin.geogames.service.stats.RoundAnswered;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Counts games, rounds, challenges and a few health events for the metrics export. Tags are
 * enum-like values only, never an id, a round key, a score, a date or anything a player typed. The
 * counters are in-process and reset on restart.
 *
 * <p>Every series is registered at zero at start-up: the free Render instance restarts often, and
 * PromQL's {@code increase()} ignores a series' first sample, so the first event after each start
 * would otherwise be lost.
 */
@Component
public class GameMetrics {

  /** Why a new game was refused. */
  public enum RejectReason {
    RATE_LIMITED,
    GLOBAL_RATE,
    CAPACITY
  }

  /** Why a statistics row was dropped. */
  public enum DropReason {
    QUEUE_FULL,
    ERROR
  }

  /** What the cleanup deleted. */
  public enum DeletedKind {
    GAME,
    CHALLENGE
  }

  /** What became of a request to set initials. */
  public enum InitialsResult {
    SAVED,
    INVALID,
    BLOCKED,
    NOT_TOP5
  }

  private enum ChallengeLookup {
    FOUND,
    EXPIRED
  }

  private enum Outcome {
    BEAT,
    TIED,
    LOST
  }

  private static final String UNKNOWN = "UNKNOWN";
  private static final int TOP_BAND = 10;
  private static final List<String> TABLE_SIZE_BANDS = List.of("1", "2", "3-5", "6-10", "11+");

  private static final String GAMES_STARTED = "geogames.games.started";
  private static final String GAMES_FINISHED = "geogames.games.finished";
  private static final String GAMES_REJECTED = "geogames.games.rejected";
  private static final String ROUNDS_ANSWERED = "geogames.rounds.answered";
  private static final String ROUNDS_HINTS = "geogames.rounds.hints";
  private static final String ROUNDS_WRONG_GUESSES = "geogames.rounds.wrong_guesses";
  private static final String ROUNDS_GUESS_REPORTED = "geogames.rounds.guess_reported";
  private static final String CHALLENGES_CREATED = "geogames.challenges.created";
  private static final String CHALLENGES_OPENED = "geogames.challenges.opened";
  private static final String CHALLENGES_STARTED = "geogames.challenges.started";
  private static final String CHALLENGES_FINISHED = "geogames.challenges.finished";
  private static final String CHALLENGES_ENTRIES = "geogames.challenges.entries";
  private static final String CHALLENGES_INITIALS = "geogames.challenges.initials";
  private static final String STATS_DROPPED = "geogames.stats.dropped";
  private static final String CLEANUP_DELETED = "geogames.cleanup.deleted";

  private final MeterRegistry registry;

  public GameMetrics(MeterRegistry registry) {
    this.registry = registry;
    registerAtZero();
  }

  /** {@code gameMode} is {@code RANDOM}, {@code DAILY} or a game type name. */
  public void gameStarted(String gameMode, Difficulty difficulty, GameOrigin origin) {
    counter(
            GAMES_STARTED,
            "mode",
            gameMode,
            "difficulty",
            difficultyName(difficulty),
            "origin",
            origin.name())
        .increment();
  }

  /** Never log the event: its {@code toString} prints the play id. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRoundAnswered(RoundAnswered event) {
    String gameType = event.gameType().name();
    String difficulty = difficultyName(event.difficulty());
    counter(
            ROUNDS_ANSWERED,
            "game_type",
            gameType,
            "difficulty",
            difficulty,
            "answer_state",
            event.answerState().name(),
            "hinted",
            Boolean.toString(event.hintsUsed() > 0))
        .increment();
    count(ROUNDS_HINTS, gameType, difficulty, event.hintsUsed());
    if (event.wrongGuesses() != null) {
      count(ROUNDS_WRONG_GUESSES, gameType, difficulty, event.wrongGuesses());
      count(ROUNDS_GUESS_REPORTED, gameType, difficulty, 1);
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onGameFinished(GameFinished event) {
    String difficulty = difficultyName(event.difficulty());
    int band = Math.min(TOP_BAND, event.score() * TOP_BAND / event.maxScore());
    counter(
            GAMES_FINISHED,
            "origin",
            event.origin().name(),
            "difficulty",
            difficulty,
            "score_band",
            Integer.toString(band))
        .increment();
    GameFinished.ChallengeEntry entry = event.challengeEntry();
    if (entry != null) {
      challengeEntryAdded(entry.tableSize());
    }
    if (event.challengeScore() != null) {
      // No entry means the challenge had expired, so there was no table to be in the top of.
      boolean top5 = entry != null && entry.top5();
      counter(
              CHALLENGES_FINISHED,
              "difficulty",
              difficulty,
              "outcome",
              outcome(event.score(), event.challengeScore()).name(),
              "top5",
              Boolean.toString(top5))
          .increment();
    }
  }

  /** A score joined a league table, which now holds {@code tableSize} entries. */
  public void challengeEntryAdded(int tableSize) {
    counter(CHALLENGES_ENTRIES, "table_size", tableSizeBand(tableSize)).increment();
  }

  public void challengeInitials(InitialsResult result) {
    counter(CHALLENGES_INITIALS, "result", result.name()).increment();
  }

  public void challengeCreated(String gameMode, Difficulty difficulty) {
    counter(CHALLENGES_CREATED, "mode", gameMode, "difficulty", difficultyName(difficulty))
        .increment();
  }

  /** A challenge link was opened: {@code found} is false when it has expired or never existed. */
  public void challengeOpened(boolean found) {
    ChallengeLookup result = found ? ChallengeLookup.FOUND : ChallengeLookup.EXPIRED;
    counter(CHALLENGES_OPENED, "result", result.name()).increment();
  }

  public void challengeStarted(String gameMode, Difficulty difficulty) {
    counter(CHALLENGES_STARTED, "mode", gameMode, "difficulty", difficultyName(difficulty))
        .increment();
  }

  public void gameRejected(RejectReason reason) {
    counter(GAMES_REJECTED, "reason", reason.name()).increment();
  }

  public void statsDropped(DropReason reason) {
    counter(STATS_DROPPED, "reason", reason.name()).increment();
  }

  public void deleted(DeletedKind kind, long count) {
    counter(CLEANUP_DELETED, "kind", kind.name()).increment(count);
  }

  private void registerAtZero() {
    List<String> gameTypes = names(GameType.values());
    List<String> modes = Stream.concat(Stream.of("RANDOM", "DAILY"), gameTypes.stream()).toList();
    List<String> difficulties =
        Stream.concat(names(Difficulty.values()).stream(), Stream.of(UNKNOWN)).toList();
    List<String> answerStates =
        Arrays.stream(AnswerState.values())
            .filter(state -> state != AnswerState.UNANSWERED)
            .map(Enum::name)
            .toList();
    List<String> origins = names(GameOrigin.values());
    List<String> bands = new ArrayList<>();
    for (int band = 0; band <= TOP_BAND; band++) {
      bands.add(Integer.toString(band));
    }

    register(
        GAMES_STARTED,
        new Tag("mode", modes),
        new Tag("difficulty", difficulties),
        new Tag("origin", origins));
    register(
        GAMES_FINISHED,
        new Tag("origin", origins),
        new Tag("difficulty", difficulties),
        new Tag("score_band", bands));
    register(GAMES_REJECTED, new Tag("reason", names(RejectReason.values())));
    register(
        ROUNDS_ANSWERED,
        new Tag("game_type", gameTypes),
        new Tag("difficulty", difficulties),
        new Tag("answer_state", answerStates),
        new Tag("hinted", List.of("false", "true")));
    for (String name : List.of(ROUNDS_HINTS, ROUNDS_WRONG_GUESSES, ROUNDS_GUESS_REPORTED)) {
      register(name, new Tag("game_type", gameTypes), new Tag("difficulty", difficulties));
    }
    for (String name : List.of(CHALLENGES_CREATED, CHALLENGES_STARTED)) {
      register(name, new Tag("mode", modes), new Tag("difficulty", difficulties));
    }
    register(CHALLENGES_OPENED, new Tag("result", names(ChallengeLookup.values())));
    register(
        CHALLENGES_FINISHED,
        new Tag("difficulty", difficulties),
        new Tag("outcome", names(Outcome.values())),
        new Tag("top5", List.of("false", "true")));
    register(CHALLENGES_ENTRIES, new Tag("table_size", TABLE_SIZE_BANDS));
    register(CHALLENGES_INITIALS, new Tag("result", names(InitialsResult.values())));
    register(STATS_DROPPED, new Tag("reason", names(DropReason.values())));
    register(CLEANUP_DELETED, new Tag("kind", names(DeletedKind.values())));
  }

  /** A tag key and every value it takes. */
  private record Tag(String key, List<String> values) {}

  /** Registers the counter with every combination of the tags' values. */
  private void register(String name, Tag... tags) {
    List<List<String>> combinations = List.of(List.of());
    for (Tag tag : tags) {
      List<List<String>> longer = new ArrayList<>();
      for (List<String> combination : combinations) {
        for (String value : tag.values()) {
          longer.add(Stream.concat(combination.stream(), Stream.of(tag.key(), value)).toList());
        }
      }
      combinations = longer;
    }
    combinations.forEach(keyValues -> counter(name, keyValues.toArray(String[]::new)));
  }

  private void count(String name, String gameType, String difficulty, int amount) {
    counter(name, "game_type", gameType, "difficulty", difficulty).increment(amount);
  }

  private Counter counter(String name, String... keyValues) {
    return Counter.builder(name).tags(keyValues).register(registry);
  }

  private static Outcome outcome(int score, int toBeat) {
    if (score > toBeat) {
      return Outcome.BEAT;
    }
    return score == toBeat ? Outcome.TIED : Outcome.LOST;
  }

  private static String tableSizeBand(int size) {
    if (size <= 2) {
      return Integer.toString(size);
    }
    if (size <= 5) {
      return "3-5";
    }
    return size <= 10 ? "6-10" : "11+";
  }

  private static List<String> names(Enum<?>[] values) {
    return Arrays.stream(values).map(Enum::name).toList();
  }

  private static String difficultyName(Difficulty difficulty) {
    return difficulty == null ? UNKNOWN : difficulty.name();
  }
}
