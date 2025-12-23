package com.assignments.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.assignments.model.ReservationEntity;
import com.assignments.repository.ReservationRepository;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private CreditCardPaymentService creditCardPaymentService;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationRepository, creditCardPaymentService);
    }

    @Test
    void cancelUnpaidReservations_ShouldCancelPendingUnpaidReservations() {
        // Given
        ReservationEntity unpaidReservation = new ReservationEntity();
        unpaidReservation.setReservationId("RES001");
        unpaidReservation.setStatus(ReservationEntity.ReservationStatus.PENDING_PAYMENT);
        unpaidReservation.setTotalAmount(new BigDecimal("100.00"));
        unpaidReservation.setAmountReceived(BigDecimal.ZERO);

        ReservationEntity paidReservation = new ReservationEntity();
        paidReservation.setReservationId("RES002");
        paidReservation.setStatus(ReservationEntity.ReservationStatus.PENDING_PAYMENT); // Still pending but fully paid
                                                                                        // (maybe status update lagged?)

        paidReservation.setTotalAmount(new BigDecimal("100.00"));
        paidReservation.setAmountReceived(new BigDecimal("100.00"));

        ReservationEntity partialPaidReservation = new ReservationEntity();
        partialPaidReservation.setReservationId("RES003");
        partialPaidReservation.setStatus(ReservationEntity.ReservationStatus.PENDING_PAYMENT);
        partialPaidReservation.setTotalAmount(new BigDecimal("100.00"));
        partialPaidReservation.setAmountReceived(new BigDecimal("50.00"));

        when(reservationRepository.findByStatusAndModeOfPaymentAndStartDateBefore(
                eq(ReservationEntity.ReservationStatus.PENDING_PAYMENT),
                eq(ReservationEntity.ModeOfPayment.BANK_TRANSFER),
                any(LocalDate.class)))
                .thenReturn(Arrays.asList(unpaidReservation, paidReservation, partialPaidReservation));

        // When
        reservationService.cancelUnpaidReservations();

        // Then
        // Should cancel RES001
        verify(reservationRepository).save(unpaidReservation);
        assertEquals(ReservationEntity.ReservationStatus.CANCELLED, unpaidReservation.getStatus());

        // Should cancel RES003 (partial payment is still < total)
        verify(reservationRepository).save(partialPaidReservation);
        assertEquals(ReservationEntity.ReservationStatus.CANCELLED, partialPaidReservation.getStatus());

        // Should NOT cancel RES002 (fully paid)
        verify(reservationRepository, never()).save(paidReservation);
    }
}
