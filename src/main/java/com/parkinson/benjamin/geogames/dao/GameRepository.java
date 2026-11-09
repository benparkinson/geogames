package com.parkinson.benjamin.geogames.dao;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface GameRepository extends CrudRepository<GameEntity, Long> {

  Optional<GameEntity> findByPublicId(UUID publicId);

  /**
   * Deletes games created before the cutoff. A derived delete, not a bulk query, so the cascade
   * removes each game's rounds too.
   */
  long deleteByCreatedAtBefore(Instant cutoff);
}
