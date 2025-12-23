package com.assignments.kafka;

import com.assignments.model.BankTransferPaymentEvent;
import com.assignments.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BankTransferPaymentConsumerTest {

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private BankTransferPaymentConsumer bankTransferPaymentConsumer;

    private BankTransferPaymentEvent validEvent;

    @BeforeEach
    void setUp() {
        validEvent = new BankTransferPaymentEvent();
        validEvent.setPaymentId("PAY123");
        validEvent.setDebtorAccountNumber("ACC-123");
        validEvent.setAmountReceived(new BigDecimal("1500.00"));
        validEvent.setTransactionDescription("1234567890 P1234567");
    }

    @Test
    void consumePaymentUpdate_WithValidEvent_ShouldProcessPayment() {
        // Given
        String expectedReservationId = "P1234567";
        BigDecimal expectedAmount = new BigDecimal("1500.00");

        // When
        bankTransferPaymentConsumer.consumePaymentUpdate(validEvent);

        // Then
        verify(reservationService, times(1)).processBankTransferPayment(eq(expectedReservationId), eq(expectedAmount));
    }

    @Test
    void consumePaymentUpdate_WithInvalidDescription_ShouldNotProcessPayment() {
        // Given
        validEvent.setTransactionDescription("short");

        // When
        bankTransferPaymentConsumer.consumePaymentUpdate(validEvent);

        // Then
        verify(reservationService, never()).processBankTransferPayment(any(), any());
    }

    @Test
    void consumePaymentUpdate_WithEmptyDescription_ShouldNotProcessPayment() {
        // Given
        validEvent.setTransactionDescription("");

        // When
        bankTransferPaymentConsumer.consumePaymentUpdate(validEvent);

        // Then
        verify(reservationService, never()).processBankTransferPayment(any(), any());
    }

    @Test
    void consumePaymentUpdate_WithEmptyReservationId_ShouldNotProcessPayment() {
        // Given
        // Length is 20, but reservation ID part (indices 11-19) is spaces
        validEvent.setTransactionDescription("1234567890          ");

        // When
        bankTransferPaymentConsumer.consumePaymentUpdate(validEvent);

        // Then
        verify(reservationService, never()).processBankTransferPayment(any(), any());
    }

    @Test
    void consumePaymentUpdate_WhenEventIsNotValid_ShouldHandleException() {
        // Given
        validEvent = new BankTransferPaymentEvent();

        // When
        bankTransferPaymentConsumer.consumePaymentUpdate(validEvent);

        // Then
        verify(reservationService, never()).processBankTransferPayment(any(), any());
    }

    @Test
    void consumePaymentUpdate_WithPoisonPill_ShouldHandleExceptionGracefully() {
        // Given
        doThrow(new RuntimeException("Severe Runtime Error"))
                .when(reservationService).processBankTransferPayment(any(), any());

        // When
        bankTransferPaymentConsumer.consumePaymentUpdate(validEvent);

        // Then
        verify(reservationService, times(1)).processBankTransferPayment(any(), any());
    }
}
