package com.ticketblitz.booking.dto;

import com.ticketblitz.booking.entity.BookingStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class BookingResponse {
    private Long bookingId;
    private Long eventId;
    private Long userId;
    private int ticketCount;
    private BookingStatus status;
    private BigDecimal totalPrice;
}
