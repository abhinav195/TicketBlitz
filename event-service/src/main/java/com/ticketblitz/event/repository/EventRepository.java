package com.ticketblitz.event.repository;

import com.ticketblitz.event.entity.Event;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    @Query("""
            SELECT e FROM Event e
            WHERE e.date > CURRENT_TIMESTAMP
            AND (
              :keyword IS NULL OR :keyword = '' OR
              LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
              LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
              LOWER(e.location) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (:category IS NULL OR :category = '' OR e.category.name = :category)
            AND (:minPrice IS NULL OR e.price >= :minPrice)
            AND (:maxPrice IS NULL OR e.price <= :maxPrice)
            """)
    Page<Event> searchEventsFallback(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    // STANDARD: For reading without locking (Browsing events)
    Optional<Event> findById(Long id);

    // CRITICAL: For booking tickets.
    // PESSIMISTIC_WRITE issues a "SELECT ... FOR UPDATE" statement.
    // This physically locks the row in Postgres until the transaction finishes.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")}) // Fail fast if locked too long
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdLocked(@Param("id")Long id);
}
