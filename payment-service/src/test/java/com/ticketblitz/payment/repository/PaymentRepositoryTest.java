package com.ticketblitz.payment.repository;

import com.ticketblitz.payment.entity.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PaymentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("existsByBookingId: Should return true when payment exists")
    void existsByBookingIdShouldReturnTrueWhenPaymentExists() {
        // Arrange
        Payment payment = createPayment(101L, 10L, "250.00", "SUCCESS", "ch_test_12345");
        entityManager.persist(payment);
        entityManager.flush();

        // Act
        boolean exists = paymentRepository.existsByBookingId(101L);

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByBookingId: Should return false when payment does not exist")
    void existsByBookingIdShouldReturnFalseWhenPaymentDoesNotExist() {
        // Act
        boolean exists = paymentRepository.existsByBookingId(999L);

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByBookingId: Should return false for null bookingId")
    void existsByBookingIdShouldReturnFalseForNull() {
        // Act
        boolean exists = paymentRepository.existsByBookingId(null);

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByBookingId: Should handle multiple payments with different bookingIds")
    void existsByBookingIdShouldHandleMultiplePayments() {
        // Arrange
        entityManager.persist(createPayment(101L, 10L, "100.00", "SUCCESS", "ch_1"));
        entityManager.persist(createPayment(102L, 11L, "200.00", "SUCCESS", "ch_2"));
        entityManager.persist(createPayment(103L, 12L, "300.00", "FAILED", "ch_3"));
        entityManager.flush();

        // Act & Assert
        assertThat(paymentRepository.existsByBookingId(101L)).isTrue();
        assertThat(paymentRepository.existsByBookingId(102L)).isTrue();
        assertThat(paymentRepository.existsByBookingId(103L)).isTrue();
        assertThat(paymentRepository.existsByBookingId(104L)).isFalse();
    }

    @Test
    @DisplayName("save: Should persist payment successfully")
    void saveShouldPersistPayment() {
        // Arrange
        Payment payment = createPayment(102L, 11L, "100.00", "FAILED", "FALLBACK_ERROR");

        // Act
        Payment saved = paymentRepository.save(payment);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getBookingId()).isEqualTo(102L);
        assertThat(saved.getUserId()).isEqualTo(11L);
        assertThat(saved.getAmount()).isEqualByComparingTo("100.00");
        assertThat(saved.getStatus()).isEqualTo("FAILED");
        assertThat(saved.getStripePaymentId()).isEqualTo("FALLBACK_ERROR");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("save: Should update existing payment")
    void saveShouldUpdateExistingPayment() {
        // Arrange
        Payment payment = createPayment(103L, 12L, "500.00", "SUCCESS", "ch_old");
        Payment saved = entityManager.persist(payment);
        entityManager.flush();

        Long savedId = saved.getId();

        // Act - Update the payment
        saved.setStatus("FAILED");
        saved.setStripePaymentId("ch_new");
        Payment updated = paymentRepository.save(saved);

        // Assert
        assertThat(updated.getId()).isEqualTo(savedId);
        assertThat(updated.getStatus()).isEqualTo("FAILED");
        assertThat(updated.getStripePaymentId()).isEqualTo("ch_new");
    }

    @Test
    @DisplayName("findById: Should retrieve payment by ID")
    void findByIdShouldRetrievePayment() {
        // Arrange
        Payment payment = createPayment(103L, 12L, "500.00", "SUCCESS", "ch_test_67890");
        Payment saved = entityManager.persist(payment);
        entityManager.flush();

        // Act
        Optional<Payment> found = paymentRepository.findById(saved.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getBookingId()).isEqualTo(103L);
        assertThat(found.get().getUserId()).isEqualTo(12L);
        assertThat(found.get().getAmount()).isEqualByComparingTo("500.00");
        assertThat(found.get().getStripePaymentId()).isEqualTo("ch_test_67890");
    }

    @Test
    @DisplayName("findById: Should return empty for non-existent ID")
    void findByIdShouldReturnEmptyForNonExistentId() {
        // Act
        Optional<Payment> found = paymentRepository.findById(99999L);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findAll: Should retrieve all payments")
    void findAllShouldRetrieveAllPayments() {
        // Arrange
        entityManager.persist(createPayment(101L, 10L, "100.00", "SUCCESS", "ch_1"));
        entityManager.persist(createPayment(102L, 11L, "200.00", "SUCCESS", "ch_2"));
        entityManager.persist(createPayment(103L, 12L, "300.00", "FAILED", "ch_3"));
        entityManager.flush();

        // Act
        List<Payment> payments = paymentRepository.findAll();

        // Assert
        assertThat(payments).hasSize(3);
    }

    @Test
    @DisplayName("delete: Should delete payment")
    void deleteShouldRemovePayment() {
        // Arrange
        Payment payment = createPayment(104L, 13L, "400.00", "SUCCESS", "ch_delete");
        Payment saved = entityManager.persist(payment);
        entityManager.flush();

        Long savedId = saved.getId();

        // Act
        paymentRepository.delete(saved);
        entityManager.flush();

        // Assert
        Optional<Payment> found = paymentRepository.findById(savedId);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("count: Should return correct count of payments")
    void countShouldReturnCorrectCount() {
        // Arrange
        entityManager.persist(createPayment(101L, 10L, "100.00", "SUCCESS", "ch_1"));
        entityManager.persist(createPayment(102L, 11L, "200.00", "SUCCESS", "ch_2"));
        entityManager.flush();

        // Act
        long count = paymentRepository.count();

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("PaymentRepository: Should have @Repository annotation")
    void paymentRepositoryShouldHaveRepositoryAnnotation() {
        assertThat(PaymentRepository.class.isAnnotationPresent(
                org.springframework.stereotype.Repository.class
        )).isTrue();
    }

    // Helper method to create Payment entities
    private Payment createPayment(Long bookingId, Long userId, String amount, String status, String stripePaymentId) {
        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setUserId(userId);
        payment.setAmount(new BigDecimal(amount));
        payment.setStatus(status);
        payment.setStripePaymentId(stripePaymentId);
        return payment;
    }
}
