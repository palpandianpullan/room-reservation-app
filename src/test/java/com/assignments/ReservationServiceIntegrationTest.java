package com.assignments;

import com.assignments.model.BankTransferPaymentEvent;
import com.assignments.model.ReservationEntity;
import com.assignments.repository.ReservationRepository;
import com.assignments.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Room Reservation Service
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "credit.card.payment.service.url=http://localhost:9090/credit-card-payment-api"
})
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void testCashPaymentReservation() {
        // Given
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = startDate.plusDays(3);

        // When
        ReservationEntity reservation = reservationService.confirmReservation(
                "John Doe",
                "101A",
                startDate,
                endDate,
                ReservationEntity.RoomSegment.LARGE,
                ReservationEntity.ModeOfPayment.CASH,
                null);

        // Then
        assertNotNull(reservation);
        assertNotNull(reservation.getReservationId());
        assertEquals(ReservationEntity.ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(new BigDecimal("600.00"), reservation.getTotalAmount()); // 3 days * $200/day
        assertEquals(reservation.getTotalAmount(), reservation.getAmountReceived());
    }

    @Test
    void testBankTransferReservation() {
        // Given
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = startDate.plusDays(2);

        // When
        ReservationEntity reservation = reservationService.confirmReservation(
                "Jane Smith",
                "202B",
                startDate,
                endDate,
                ReservationEntity.RoomSegment.MEDIUM,
                ReservationEntity.ModeOfPayment.BANK_TRANSFER,
                "TXN123456");

        // Then
        assertNotNull(reservation);
        assertEquals(ReservationEntity.ReservationStatus.PENDING_PAYMENT, reservation.getStatus());
        assertEquals(new BigDecimal("300.00"), reservation.getTotalAmount()); // 2 days * $150/day
        assertEquals(BigDecimal.ZERO, reservation.getAmountReceived());
    }

    @Test
    void testBankTransferPaymentProcessing() {
        // Given - Create a reservation
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = startDate.plusDays(2);

        ReservationEntity reservation = reservationService.confirmReservation(
                "Bob Johnson",
                "303C",
                startDate,
                endDate,
                ReservationEntity.RoomSegment.SMALL,
                ReservationEntity.ModeOfPayment.BANK_TRANSFER,
                null);

        String reservationId = reservation.getReservationId();
        BigDecimal totalAmount = reservation.getTotalAmount(); // 2 days * $100 = $200

        // When - Process full payment
        reservationService.processBankTransferPayment(reservationId, totalAmount);

        // Then
        ReservationEntity updatedReservation = reservationRepository.findById(reservationId).orElseThrow();
        assertEquals(ReservationEntity.ReservationStatus.CONFIRMED, updatedReservation.getStatus());
        assertEquals(totalAmount, updatedReservation.getAmountReceived());
    }

    @Test
    void testPartialBankTransferPayment() {
        // Given
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = startDate.plusDays(4);

        ReservationEntity reservation = reservationService.confirmReservation(
                "Alice Williams",
                "404D",
                startDate,
                endDate,
                ReservationEntity.RoomSegment.EXTRA_LARGE,
                ReservationEntity.ModeOfPayment.BANK_TRANSFER,
                null);

        String reservationId = reservation.getReservationId();
        BigDecimal totalAmount = reservation.getTotalAmount(); // 4 days * $300 = $1200

        // When - Process partial payment
        BigDecimal partialPayment = new BigDecimal("500.00");
        reservationService.processBankTransferPayment(reservationId, partialPayment);

        // Then - Should still be pending
        ReservationEntity updatedReservation = reservationRepository.findById(reservationId).orElseThrow();
        assertEquals(ReservationEntity.ReservationStatus.PENDING_PAYMENT, updatedReservation.getStatus());
        assertEquals(partialPayment, updatedReservation.getAmountReceived());

        // When - Process remaining payment
        BigDecimal remainingPayment = totalAmount.subtract(partialPayment);
        reservationService.processBankTransferPayment(reservationId, remainingPayment);

        // Then - Should be confirmed
        updatedReservation = reservationRepository.findById(reservationId).orElseThrow();
        assertEquals(ReservationEntity.ReservationStatus.CONFIRMED, updatedReservation.getStatus());
        assertEquals(totalAmount, updatedReservation.getAmountReceived());
    }

    @Test
    void testReservationDurationValidation() {
        // Given - More than 30 days
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = startDate.plusDays(31);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            reservationService.confirmReservation(
                    "Invalid User",
                    "505E",
                    startDate,
                    endDate,
                    ReservationEntity.RoomSegment.LARGE,
                    ReservationEntity.ModeOfPayment.CASH,
                    null);
        });
    }

    @Test
    void testBankTransferEventExtractReservationId() {
        // Given
        BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                "PAY123",
                "ACC789",
                new BigDecimal("450.00"),
                "1401541457 P4145478");

        // When
        String reservationId = event.extractReservationId();

        // Then
        assertEquals("P4145478", reservationId);
    }
}
