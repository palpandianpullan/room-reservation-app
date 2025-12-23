package com.assignments.controller;

import com.assignments.api.ReservationsApi;
import com.assignments.model.ReservationEntity;
import com.assignments.model.ReservationRequest;
import com.assignments.model.ReservationResponse;
import com.assignments.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller implementing the Reservations API
 */
@RestController
public class ReservationController implements ReservationsApi {

    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public ResponseEntity<ReservationResponse> confirmReservation(ReservationRequest reservationRequest) {
        logger.info("Received reservation request for customer: {}, room: {}, payment mode: {}",
                reservationRequest.getCustomerName(),
                reservationRequest.getRoomNumber(),
                reservationRequest.getModeOfPayment());

        // Convert API enums to entity enums
        ReservationEntity.RoomSegment roomSegment = convertRoomSegment(reservationRequest.getRoomSegment());
        ReservationEntity.ModeOfPayment modeOfPayment = convertModeOfPayment(reservationRequest.getModeOfPayment());

        // Create confirmation on reservation
        ReservationEntity reservation = reservationService.confirmReservation(
                reservationRequest.getCustomerName(),
                reservationRequest.getRoomNumber(),
                reservationRequest.getStartDate(),
                reservationRequest.getEndDate(),
                roomSegment,
                modeOfPayment,
                reservationRequest.getPaymentReference());

        // Build response
        ReservationResponse response = new ReservationResponse();
        response.setReservationId(reservation.getReservationId());
        response.setStatus(convertStatus(reservation.getStatus()));

        logger.info("Reservation created successfully: {}, status: {}",
                reservation.getReservationId(), reservation.getStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * Convert API RoomSegment enum to Entity enum
     */
    private ReservationEntity.RoomSegment convertRoomSegment(ReservationRequest.RoomSegmentEnum apiSegment) {
        return ReservationEntity.RoomSegment.valueOf(apiSegment.name());
    }

    /**
     * Convert API ModeOfPayment enum to Entity enum
     */
    private ReservationEntity.ModeOfPayment convertModeOfPayment(ReservationRequest.ModeOfPaymentEnum apiMode) {
        return ReservationEntity.ModeOfPayment.valueOf(apiMode.name());
    }

    /**
     * Convert Entity Status enum to API enum
     */
    private ReservationResponse.StatusEnum convertStatus(ReservationEntity.ReservationStatus entityStatus) {
        return ReservationResponse.StatusEnum.valueOf(entityStatus.name());
    }
}
