package com.ticketblitz.payment.repository;

import com.ticketblitz.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByBookingId(Long bookingId);
}
