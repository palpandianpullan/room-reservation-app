package com.assignments.repository;

import com.assignments.model.ReservationEntity;
import com.assignments.model.ReservationEntity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for reservation data access
 */
@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, String> {

    /**
     * Find all reservations with PENDING_PAYMENT status and bank transfer payment
     * mode
     * where start date is within the specified number of days
     */
    List<ReservationEntity> findByStatusAndModeOfPaymentAndStartDateBefore(
            ReservationStatus status,
            ReservationEntity.ModeOfPayment modeOfPayment,
            LocalDate startDate);

    /**
     * Find reservation by reservation ID
     */
    Optional<ReservationEntity> findByReservationId(String reservationId);
}
