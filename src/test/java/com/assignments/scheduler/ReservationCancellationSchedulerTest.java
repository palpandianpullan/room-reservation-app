package com.assignments.scheduler;

import com.assignments.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationCancellationSchedulerTest {

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationCancellationScheduler scheduler;

    @Test
    void testCancelUnpaidReservations_Success() {
        // Given
        doNothing().when(reservationService).cancelUnpaidReservations();

        // When
        scheduler.cancelUnpaidReservations();

        // Then
        verify(reservationService, times(1)).cancelUnpaidReservations();
    }

    @Test
    void testCancelUnpaidReservations_ExceptionHandling() {
        // Given
        doThrow(new RuntimeException("Database connection failed"))
                .when(reservationService).cancelUnpaidReservations();

        // When
        // The scheduler catches the exception, so this should not throw
        scheduler.cancelUnpaidReservations();

        // Then
        verify(reservationService, times(1)).cancelUnpaidReservations();
    }
}
