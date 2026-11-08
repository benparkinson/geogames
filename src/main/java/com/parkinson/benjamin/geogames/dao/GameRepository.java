package com.parkinson.benjamin.geogames.dao;

import java.time.Instant;
import org.springframework.data.repository.CrudRepository;

public interface GameRepository extends CrudRepository<GameEntity, Long> {

  /**
   * Deletes games created before the cutoff. A derived delete, not a bulk query, so the cascade
   * removes each game's rounds and its join-table rows too.
   */
  long deleteByCreatedAtBefore(Instant cutoff);
}
