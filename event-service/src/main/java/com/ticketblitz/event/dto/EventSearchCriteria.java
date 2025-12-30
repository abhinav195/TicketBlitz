package com.ticketblitz.event.dto;

import java.math.BigDecimal;

public record EventSearchCriteria(
        String query,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {}
