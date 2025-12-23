package com.assignments.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.assignments.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import reactor.core.publisher.Mono;

/**
 * Service for verifying credit card payments via external API
 */
@Service
public class CreditCardPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(CreditCardPaymentService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${credit.card.payment.service.url:http://localhost:9090/credit-card-payment-api}")
    private String paymentServiceUrl;

    public CreditCardPaymentService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Verify credit card payment status
     * 
     * @param paymentReference The payment reference to verify
     * @return true if payment is CONFIRMED, false otherwise
     * @throws RuntimeException if payment service fails or payment is not found
     */
    @CircuitBreaker(name = "creditCardPaymentService", fallbackMethod = "verifyPaymentFallback")
    public boolean verifyPayment(String paymentReference) {
        logger.info("Verifying credit card payment for reference: {}", paymentReference);

        try {
            String requestBody = String.format("{\"paymentReference\":\"%s\"}", paymentReference);

            String response = webClient.post()
                    .uri(paymentServiceUrl + "/payment-status")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                            clientResponse -> Mono.error(new RuntimeException("Payment not found or invalid")))
                    .onStatus(status -> status.is5xxServerError(),
                            clientResponse -> Mono.error(new RuntimeException("Payment service unavailable")))
                    .bodyToMono(String.class)
                    .block();

            // Parse response to check status
            JsonNode jsonNode = objectMapper.readTree(response);
            String status = jsonNode.get("status").asText();

            logger.info("Payment status for reference {}: {}", paymentReference, status);

            if ("CONFIRMED".equals(status)) {
                return true;
            } else if ("REJECTED".equals(status)) {
                throw new RuntimeException("Credit card payment was rejected");
            } else {
                throw new RuntimeException("Unknown payment status: " + status);
            }

        } catch (Exception e) {
            logger.error("Error verifying credit card payment: {}", e.getMessage());
            throw new RuntimeException("Failed to verify credit card payment: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method for verifyPayment circuit breaker
     */
    public boolean verifyPaymentFallback(String paymentReference, Exception e) {
        logger.error("Circuit breaker triggered for credit card payment verification. Reference: {}, Error: {}",
                paymentReference, e.getMessage());
        // Return false or throw a custom exception depending on business rules
        // For now, we'll throw an ExternalServiceException to indicate service
        // unavailability
        throw new ExternalServiceException(
                "Credit card payment verification service is currently unavailable. Please try again later.");
    }
}
