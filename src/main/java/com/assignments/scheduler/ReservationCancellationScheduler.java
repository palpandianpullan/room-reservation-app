package com.assignments.scheduler;

import com.assignments.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to automatically cancel unpaid bank transfer reservations
 * Runs daily to check for reservations that haven't received full payment
 * 2 days before the start date
 */
@Component
public class ReservationCancellationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReservationCancellationScheduler.class);

    private final ReservationService reservationService;

    public ReservationCancellationScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Run daily at 2 AM to cancel unpaid reservations
     * Cron expression: second minute hour day month weekday
     */
    @Scheduled(cron = "${reservation.cancellation.cron:0 0 2 * * *}")
    public void cancelUnpaidReservations() {
        logger.info("Running scheduled task to cancel unpaid reservations");

        try {
            reservationService.cancelUnpaidReservations();
            logger.info("Completed scheduled cancellation task");
        } catch (Exception e) {
            logger.error("Error during scheduled cancellation task: {}", e.getMessage(), e);
        }
    }
}
