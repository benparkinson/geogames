package com.parkinson.benjamin.geogames.service.challenge;

import com.parkinson.benjamin.geogames.api.BadRequestException;
import com.parkinson.benjamin.geogames.api.ChallengeCreationResponse;
import com.parkinson.benjamin.geogames.api.ChallengeEntryView;
import com.parkinson.benjamin.geogames.api.ChallengeResponse;
import com.parkinson.benjamin.geogames.api.ChallengeTableResponse;
import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.service.ChallengeLink;
import com.parkinson.benjamin.geogames.service.ChallengeSource;
import com.parkinson.benjamin.geogames.service.GameService;
import com.parkinson.benjamin.geogames.service.metrics.GameMetrics;
import com.parkinson.benjamin.geogames.service.metrics.GameMetrics.InitialsResult;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Challenges a friend to play the rounds of a finished game. A game has at most one challenge: the
 * one it made, or the one it was started from, and sharing it again reuses that link.
 */
@Service
public class ChallengeService {

  /** The rows of the league table, and the ranks that may enter initials. */
  public static final int TABLE_SIZE = 5;

  private final Clock clock;
  private final ChallengeStore store;
  private final ChallengeEntryStore entries;
  private final GameService gameService;
  private final GameMetrics metrics;
  private final Initials initials;

  public ChallengeService(
      Clock clock,
      ChallengeStore store,
      ChallengeEntryStore entries,
      GameService gameService,
      GameMetrics metrics,
      Initials initials) {
    this.clock = clock;
    this.store = store;
    this.entries = entries;
    this.gameService = gameService;
    this.metrics = metrics;
    this.initials = initials;
  }

  /**
   * The game's challenge, making one with this game's score as its first entry if it has none (or
   * it has expired). One transaction, so a challenge is never stored without its entry.
   *
   * @throws ChallengeNotFoundException for an unknown game
   * @throws ChallengeConflictException for an unfinished game
   * @throws BadRequestException for a daily game, which has its own share
   */
  @Transactional
  public ChallengeCreationResponse create(UUID gameId) {
    ChallengeSource source =
        gameService
            .challengeSource(gameId)
            .orElseThrow(() -> new ChallengeNotFoundException("No such game"));
    LocalDate today = LocalDate.now(clock);
    Optional<StoredChallenge> existing =
        Optional.ofNullable(source.challengeId()).flatMap(store::findById);
    if (existing.isPresent()) {
      store.touch(existing.get().id(), today);
      return new ChallengeCreationResponse(existing.get().challenge().publicId());
    }
    UUID publicId = UUID.randomUUID();
    long challengeId =
        store.insert(
            new Challenge(
                publicId,
                source.gameMode(),
                source.difficulty(),
                source.rounds(),
                source.maxScore(),
                today,
                today));
    gameService.linkChallenge(gameId, challengeId, entries.insert(challengeId, source.score()));
    metrics.challengeCreated(source.gameMode(), source.difficulty());
    metrics.challengeEntryAdded(1);
    return new ChallengeCreationResponse(publicId);
  }

  public ChallengeResponse find(UUID id) {
    Optional<StoredChallenge> found = store.find(id);
    metrics.challengeOpened(found.isPresent());
    StoredChallenge stored = found.orElseThrow(ChallengeService::expired);
    Challenge challenge = stored.challenge();
    return new ChallengeResponse(
        challenge.gameMode(),
        challenge.difficulty().name(),
        challenge.rounds().size(),
        entries.topScore(stored.id()),
        challenge.maxScore(),
        rows(entries.top(stored.id(), TABLE_SIZE), entry -> null),
        entries.count(stored.id()));
  }

  /**
   * The league table of the game's challenge.
   *
   * @throws ChallengeNotFoundException for an unknown game, or one with no challenge (or an expired
   *     one)
   */
  public ChallengeTableResponse table(UUID gameId) {
    ChallengeLink link = link(gameId);
    StoredChallenge stored =
        Optional.ofNullable(link.challengeId())
            .flatMap(store::findById)
            .orElseThrow(() -> new ChallengeNotFoundException("This game has no challenge"));
    List<ChallengeEntryStore.Entry> top = entries.top(stored.id(), TABLE_SIZE);
    Long own = link.entryId();
    boolean inTop = own != null && top.stream().anyMatch(entry -> entry.id() == own);
    ChallengeEntryView yours =
        own == null || inTop
            ? null
            : entries
                .find(own)
                .map(
                    e ->
                        new ChallengeEntryView(
                            entries.rank(own), e.score(), initials.visible(e.initials()), true))
                .orElse(null);
    return new ChallengeTableResponse(
        stored.challenge().publicId(),
        stored.challenge().maxScore(),
        entries.count(stored.id()),
        link.creator(),
        rows(top, entry -> own != null && entry.id() == own),
        yours,
        inTop);
  }

  /**
   * Sets the initials on the game's entry, which must rank in the top of the table. They can be
   * changed for as long as the game exists.
   *
   * @throws BadRequestException for initials {@link Initials} rejects
   * @throws ChallengeNotFoundException for an unknown game, or one with no entry
   * @throws ChallengeConflictException for an entry below the top of the table
   */
  public void setInitials(UUID gameId, String text) {
    String normalised = normalise(text);
    Long entryId = link(gameId).entryId();
    if (entryId == null) {
      throw new ChallengeNotFoundException("This game has no challenge score");
    }
    if (entries.rank(entryId) > TABLE_SIZE) {
      metrics.challengeInitials(InitialsResult.NOT_TOP5);
      throw new ChallengeConflictException("Only the top " + TABLE_SIZE + " can enter initials");
    }
    entries.setInitials(entryId, normalised);
    metrics.challengeInitials(InitialsResult.SAVED);
  }

  /** {@link Initials#normalise}, counting what it rejects. */
  private String normalise(String text) {
    try {
      return initials.normalise(text);
    } catch (InitialsRejectedException e) {
      metrics.challengeInitials(
          switch (e.problem()) {
            case INVALID -> InitialsResult.INVALID;
            case BLOCKED -> InitialsResult.BLOCKED;
          });
      throw e;
    }
  }

  private ChallengeLink link(UUID gameId) {
    return gameService
        .challengeLink(gameId)
        .orElseThrow(() -> new ChallengeNotFoundException("No such game"));
  }

  /** The entries as rows, ranked by their order; {@code you} may return null to leave it out. */
  private List<ChallengeEntryView> rows(
      List<ChallengeEntryStore.Entry> top, Function<ChallengeEntryStore.Entry, Boolean> you) {
    return IntStream.range(0, top.size())
        .mapToObj(
            i -> {
              ChallengeEntryStore.Entry entry = top.get(i);
              return new ChallengeEntryView(
                  i + 1, entry.score(), initials.visible(entry.initials()), you.apply(entry));
            })
        .toList();
  }

  /** A new game with the challenge's rounds, mode and difficulty; each call makes another. */
  public GameCreationResponse createGame(UUID id) {
    StoredChallenge stored = store.find(id).orElseThrow(ChallengeService::expired);
    Challenge challenge = stored.challenge();
    // Touched first, so cleanup can't delete it before the new game points at it.
    store.touch(stored.id(), LocalDate.now(clock));
    GameCreationResponse game =
        gameService.createGame(
            challenge.rounds(),
            challenge.gameMode(),
            challenge.difficulty(),
            null,
            entries.topScore(stored.id()),
            stored.id());
    metrics.challengeStarted(challenge.gameMode(), challenge.difficulty());
    return game;
  }

  private static ChallengeNotFoundException expired() {
    return new ChallengeNotFoundException("This challenge has expired");
  }
}
