package com.parkinson.benjamin.geogames.dao;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface GameRepository extends CrudRepository<GameEntity, Long> {

  Optional<GameEntity> findByPublicId(UUID publicId);

  /**
   * Deletes games created before the cutoff in one statement. The rounds go with them through the
   * foreign key's {@code on delete cascade}, not through JPA, so no game is loaded. Deleting
   * nothing is not an error, so several instances can run this at the same time.
   *
   * <p>Opens its own transaction when there is none, so a caller outside a transaction (the
   * scheduled cleanup) works.
   */
  @Transactional
  @Modifying
  @Query("delete from GameEntity g where g.createdAt < :cutoff")
  int deleteCreatedBefore(@Param("cutoff") Instant cutoff);

  /**
   * Deletes games created before the cutoff in which no round has been answered. As with {@link
   * #deleteCreatedBefore}, the rounds go through the foreign key's cascade.
   */
  @Transactional
  @Modifying
  @Query(
      "delete from GameEntity g where g.createdAt < :cutoff and not exists "
          + "(select r.id from GameEntity g2 join g2.rounds r where g2 = g "
          + "and r.answerState <> com.parkinson.benjamin.geogames.model.AnswerState.UNANSWERED)")
  int deleteUnplayedCreatedBefore(@Param("cutoff") Instant cutoff);
}
