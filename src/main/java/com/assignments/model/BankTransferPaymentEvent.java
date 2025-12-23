package com.assignments.model;

import java.math.BigDecimal;

/**
 * Event model for bank transfer payment updates from Kafka topic
 */
public class BankTransferPaymentEvent {

    private String paymentId;
    private String debtorAccountNumber;
    private BigDecimal amountReceived;
    private String transactionDescription;

    public BankTransferPaymentEvent() {
    }

    public BankTransferPaymentEvent(String paymentId, String debtorAccountNumber,
            BigDecimal amountReceived, String transactionDescription) {
        this.paymentId = paymentId;
        this.debtorAccountNumber = debtorAccountNumber;
        this.amountReceived = amountReceived;
        this.transactionDescription = transactionDescription;
    }

    /**
     * Extract reservation ID from transaction description
     * Format: <E2E unique id> <reservationId>
     * <reservationId> is 8 characters long
     * <E2E unique id> is 10 characters long
     * Example: 1401541457 P4145478
     */
    public String extractReservationId() {
        if (transactionDescription != null && transactionDescription.length() >= 19) {
            return transactionDescription.substring(11, 19).trim();
        }
        return null;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getDebtorAccountNumber() {
        return debtorAccountNumber;
    }

    public void setDebtorAccountNumber(String debtorAccountNumber) {
        this.debtorAccountNumber = debtorAccountNumber;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(BigDecimal amountReceived) {
        this.amountReceived = amountReceived;
    }

    public String getTransactionDescription() {
        return transactionDescription;
    }

    public void setTransactionDescription(String transactionDescription) {
        this.transactionDescription = transactionDescription;
    }

    @Override
    public String toString() {
        return "BankTransferPaymentEvent{" +
                "paymentId='" + paymentId + '\'' +
                ", debtorAccountNumber='" + debtorAccountNumber + '\'' +
                ", amountReceived=" + amountReceived +
                ", transactionDescription='" + transactionDescription + '\'' +
                '}';
    }
}
