package com.assignments.service;

import com.assignments.model.ReservationEntity;
import com.assignments.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Core business logic for managing reservations
 */
@Service
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    private static final int MAX_RESERVATION_DAYS = 30;

    private final ReservationRepository reservationRepository;
    private final CreditCardPaymentService creditCardPaymentService;

    @Value("${reservation.pricing.small:100.00}")
    private BigDecimal priceSmall;

    @Value("${reservation.pricing.medium:150.00}")
    private BigDecimal priceMedium;

    @Value("${reservation.pricing.large:200.00}")
    private BigDecimal priceLarge;

    @Value("${reservation.pricing.extra-large:300.00}")
    private BigDecimal priceExtraLarge;

    public ReservationService(ReservationRepository reservationRepository,
            CreditCardPaymentService creditCardPaymentService) {
        this.reservationRepository = reservationRepository;
        this.creditCardPaymentService = creditCardPaymentService;
    }

    /**
     * Create a new reservation based on payment mode
     */
    @Transactional
    public ReservationEntity confirmReservation(
            String customerName,
            String roomNumber,
            LocalDate startDate,
            LocalDate endDate,
            ReservationEntity.RoomSegment roomSegment,
            ReservationEntity.ModeOfPayment modeOfPayment,
            String paymentReference) {

        // Validate reservation duration
        validateReservationDuration(startDate, endDate);

        // Create reservation entity
        ReservationEntity reservation = new ReservationEntity();
        reservation.setCustomerName(customerName);
        reservation.setRoomNumber(roomNumber);
        reservation.setStartDate(startDate);
        reservation.setEndDate(endDate);
        reservation.setRoomSegment(roomSegment);
        reservation.setModeOfPayment(modeOfPayment);
        reservation.setPaymentReference(paymentReference);

        // Calculate total amount based on room segment and duration
        BigDecimal totalAmount = calculateTotalAmount(roomSegment, startDate, endDate);
        reservation.setTotalAmount(totalAmount);

        // Process based on payment mode
        switch (modeOfPayment) {
            case CASH:
                // Cash payments are confirmed immediately
                reservation.setStatus(ReservationEntity.ReservationStatus.CONFIRMED);
                reservation.setAmountReceived(totalAmount);
                logger.info("Cash payment - Reservation confirmed immediately: {}", reservation.getReservationId());
                break;

            case CREDIT_CARD:
                // Verify credit card payment with external service
                if (paymentReference == null || paymentReference.isEmpty()) {
                    throw new IllegalArgumentException("Payment reference is required for credit card payments");
                }
                boolean paymentConfirmed = creditCardPaymentService.verifyPayment(paymentReference);
                if (paymentConfirmed) {
                    reservation.setStatus(ReservationEntity.ReservationStatus.CONFIRMED);
                    reservation.setAmountReceived(totalAmount);
                    logger.info("Credit card payment confirmed: {}", reservation.getReservationId());
                } else {
                    throw new RuntimeException("Credit card payment verification failed");
                }
                break;

            case BANK_TRANSFER:
                // Bank transfer reservations start as pending
                reservation.setStatus(ReservationEntity.ReservationStatus.PENDING_PAYMENT);
                logger.info("Bank transfer - Reservation pending payment: {}", reservation.getReservationId());
                break;

            default:
                throw new IllegalArgumentException("Unsupported payment mode: " + modeOfPayment);
        }

        return reservationRepository.save(reservation);
    }

    /**
     * Validate that reservation duration does not exceed 30 days
     */
    private void validateReservationDuration(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days > MAX_RESERVATION_DAYS) {
            throw new IllegalArgumentException(
                    String.format("Reservation duration cannot exceed %d days. Requested: %d days",
                            MAX_RESERVATION_DAYS, days));
        }
    }

    /**
     * Calculate total amount based on room segment and duration
     */
    private BigDecimal calculateTotalAmount(ReservationEntity.RoomSegment segment,
            LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate);

        // Price per day based on room segment
        BigDecimal pricePerDay;
        switch (segment) {
            case SMALL:
                pricePerDay = priceSmall;
                break;
            case MEDIUM:
                pricePerDay = priceMedium;
                break;
            case LARGE:
                pricePerDay = priceLarge;
                break;
            case EXTRA_LARGE:
                pricePerDay = priceExtraLarge;
                break;
            default:
                pricePerDay = priceSmall;
        }

        return pricePerDay.multiply(BigDecimal.valueOf(days));
    }

    /**
     * Process bank transfer payment update
     */
    @Transactional
    public void processBankTransferPayment(String reservationId, BigDecimal amountReceived) {
        logger.info("Processing bank transfer payment for reservation: {}, amount: {}",
                reservationId, amountReceived);

        Optional<ReservationEntity> optionalReservation = reservationRepository.findByReservationId(reservationId);

        if (optionalReservation.isEmpty()) {
            logger.warn("Reservation not found: {}", reservationId);
            return;
        }

        ReservationEntity reservation = optionalReservation.get();

        // Only process if reservation is pending payment
        if (reservation.getStatus() != ReservationEntity.ReservationStatus.PENDING_PAYMENT) {
            logger.warn("Reservation {} is not in PENDING_PAYMENT status. Current status: {}",
                    reservationId, reservation.getStatus());
            return;
        }

        // Update amount received
        BigDecimal currentAmount = reservation.getAmountReceived();
        BigDecimal newAmount = currentAmount.add(amountReceived);
        reservation.setAmountReceived(newAmount);

        // Check if full payment received
        if (newAmount.compareTo(reservation.getTotalAmount()) >= 0) {
            reservation.setStatus(ReservationEntity.ReservationStatus.CONFIRMED);
            logger.info("Reservation {} confirmed - full payment received", reservationId);
        } else {
            logger.info("Partial payment received for reservation {}. Total: {}, Received: {}",
                    reservationId, reservation.getTotalAmount(), newAmount);
        }

        reservationRepository.save(reservation);
    }

    /**
     * Cancel reservations that haven't received full payment 2 days before start
     * date
     */
    @Transactional
    public void cancelUnpaidReservations() {
        LocalDate twoDaysFromNow = LocalDate.now().plusDays(2);

        // Query for bank transfer reservations that haven't been paid and are starting
        // within 2 days
        List<ReservationEntity> pendingReservations = reservationRepository
                .findByStatusAndModeOfPaymentAndStartDateBefore(
                        ReservationEntity.ReservationStatus.PENDING_PAYMENT,
                        ReservationEntity.ModeOfPayment.BANK_TRANSFER,
                        twoDaysFromNow);

        for (ReservationEntity reservation : pendingReservations) {
            // Check if full payment not received
            if (reservation.getAmountReceived().compareTo(reservation.getTotalAmount()) < 0) {
                reservation.setStatus(ReservationEntity.ReservationStatus.CANCELLED);
                reservationRepository.save(reservation);
                logger.info("Cancelled reservation {} - payment not received 2 days before start date. " +
                        "Required: {}, Received: {}",
                        reservation.getReservationId(),
                        reservation.getTotalAmount(),
                        reservation.getAmountReceived());
            }
        }
    }
}
