package com.assignments.kafka;

import com.assignments.model.BankTransferPaymentEvent;
import com.assignments.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for bank transfer payment update events
 */
@Component
public class BankTransferPaymentConsumer {

    private static final Logger logger = LoggerFactory.getLogger(BankTransferPaymentConsumer.class);

    private final ReservationService reservationService;

    public BankTransferPaymentConsumer(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Listen to bank-transfer-payment-update topic and process payment events
     */
    @KafkaListener(topics = "${kafka.topic.bank-transfer-payment:bank-transfer-payment-update}", groupId = "${kafka.consumer.group-id:room-reservation-service}", containerFactory = "kafkaListenerContainerFactory")
    public void consumePaymentUpdate(BankTransferPaymentEvent event) {
        logger.info("Received bank transfer payment event: {}", event);

        try {
            // Extract reservation ID from transaction description
            String reservationId = event.extractReservationId();

            if (reservationId == null || reservationId.isEmpty()) {
                logger.warn("Could not extract reservation ID from transaction description: {}",
                        event.getTransactionDescription());
                return;
            }

            logger.info("Processing payment for reservation: {}, amount: {}",
                    reservationId, event.getAmountReceived());

            // Process the payment
            reservationService.processBankTransferPayment(reservationId, event.getAmountReceived());

        } catch (Exception e) {
            logger.error("Error processing bank transfer payment event: {}", e.getMessage(), e);
            // In production, you might want to send to a dead letter queue
        }
    }
}
