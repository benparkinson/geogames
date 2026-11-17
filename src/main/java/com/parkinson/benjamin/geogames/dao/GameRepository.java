package com.parkinson.benjamin.geogames.dao;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends CrudRepository<GameEntity, Long> {

  Optional<GameEntity> findByPublicId(UUID publicId);

  /**
   * Deletes games created before the cutoff in one statement. The rounds go with them through the
   * foreign key's {@code on delete cascade}, not through JPA, so no game is loaded. Deleting nothing
   * is not an error, so several instances can run this at the same time.
   */
  @Modifying
  @Query("delete from GameEntity g where g.createdAt < :cutoff")
  int deleteCreatedBefore(@Param("cutoff") Instant cutoff);
}
