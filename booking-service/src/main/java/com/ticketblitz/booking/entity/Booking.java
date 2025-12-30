package com.ticketblitz.booking.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking")
@Data
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long eventId;
    private int ticketCount;
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @CreationTimestamp // Automatically sets time on INSERT
    @Column(name = "booking_time", insertable = false, updatable = false)
    private LocalDateTime bookingTime; // Renamed from 'booking_time' to camelCase if preferred, or keep as is

    @Column(name = "updated_at")
    @UpdateTimestamp // Automatically updates time on UPDATE
    private LocalDateTime updatedAt;
}
