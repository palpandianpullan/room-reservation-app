package com.assignments.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA Entity for storing reservation data
 */
@Entity
@Table(name = "reservations")
public class ReservationEntity {

    @Id
    private String reservationId;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String roomNumber;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoomSegment roomSegment;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ModeOfPayment modeOfPayment;

    private String paymentReference;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private BigDecimal totalAmount;

    private BigDecimal amountReceived;

    public enum RoomSegment {
        SMALL, MEDIUM, LARGE, EXTRA_LARGE
    }

    public enum ModeOfPayment {
        CASH, BANK_TRANSFER, CREDIT_CARD
    }

    public enum ReservationStatus {
        PENDING_PAYMENT, CONFIRMED, CANCELLED
    }

    // Constructors
    public ReservationEntity() {
        this.reservationId = generateReservationId();
        this.amountReceived = BigDecimal.ZERO;
    }

    private String generateReservationId() {
        // Generate 8-character reservation ID (e.g., P4145478)
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "P" + uuid.substring(0, 7).toUpperCase();
    }

    // Getters and Setters
    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public RoomSegment getRoomSegment() {
        return roomSegment;
    }

    public void setRoomSegment(RoomSegment roomSegment) {
        this.roomSegment = roomSegment;
    }

    public ModeOfPayment getModeOfPayment() {
        return modeOfPayment;
    }

    public void setModeOfPayment(ModeOfPayment modeOfPayment) {
        this.modeOfPayment = modeOfPayment;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(BigDecimal amountReceived) {
        this.amountReceived = amountReceived;
    }
}
