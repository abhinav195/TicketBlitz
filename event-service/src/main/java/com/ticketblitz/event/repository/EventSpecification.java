package com.ticketblitz.event.repository;

import com.ticketblitz.event.entity.Event;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EventSpecification {

    // 1. Full Text Search using Postgres Native Function
    public static Specification<Event> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) return null;

            // Force the function call with specific types
            return cb.isTrue(
                    cb.function("ts_match_vq", Boolean.class,
                            root.get("searchVector"),
                            cb.function("web_search_to_tsquery", String.class,
                                    cb.literal("english"),
                                    cb.literal(keyword))
                    )
            );
        };
    }


    // 2. Category Filter
    public static Specification<Event> hasCategory(String categoryName) {
        return (root, query, cb) -> {
            if (categoryName == null || categoryName.isEmpty()) return null;
            return cb.equal(root.get("category").get("name"), categoryName);
        };
    }

    // 3. Price Range Filter
    public static Specification<Event> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("price"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("price"), min);
            return cb.lessThanOrEqualTo(root.get("price"), max);
        };
    }

    // 4. Mandatory Date Constraint (Events must be in future)
    public static Specification<Event> isUpcoming() {
        return (root, query, cb) -> cb.greaterThan(root.get("date"), LocalDateTime.now());
    }
}
