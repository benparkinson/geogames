package com.parkinson.benjamin.geogames.service.stats;

import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameOrigin;

/**
 * Published when a game's last round is first answered, and handled after the transaction commits.
 * It holds no ids.
 *
 * @param difficulty null when unknown
 * @param challengeScore the score to beat for a game started from a challenge, otherwise null
 * @param challengeEntry the league table entry this finish added, or null if it added none
 */
public record GameFinished(
    GameOrigin origin,
    Difficulty difficulty,
    int score,
    int maxScore,
    Integer challengeScore,
    ChallengeEntry challengeEntry) {

  /**
   * @param top5 whether the new entry ranked in the top of the table when it was recorded
   * @param tableSize the entries in the table, including the new one
   */
  public record ChallengeEntry(boolean top5, int tableSize) {}
}
