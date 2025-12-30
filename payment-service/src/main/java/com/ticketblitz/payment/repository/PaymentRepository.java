package com.ticketblitz.payment.repository;

import com.ticketblitz.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // You can add findByBookingId(Long id) if needed later
}
