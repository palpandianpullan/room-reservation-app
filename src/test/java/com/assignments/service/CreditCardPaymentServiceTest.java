package com.assignments.service;

import com.assignments.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CreditCardPaymentServiceTest {

    private MockWebServer mockWebServer;
    private CreditCardPaymentService creditCardPaymentService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        WebClient.Builder webClientBuilder = WebClient.builder();

        creditCardPaymentService = new CreditCardPaymentService(webClientBuilder, objectMapper);

        // Use reflection to set the private field or just rely on the fact that we
        // can't easily set it without @SpringBootTest
        // Actually, I can just mock the whole webclient but MockWebServer is better.
        // I will use a trick to inject the URL since it's private.
        try {
            java.lang.reflect.Field field = CreditCardPaymentService.class.getDeclaredField("paymentServiceUrl");
            field.setAccessible(true);
            field.set(creditCardPaymentService, baseUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testVerifyPayment_Confirmed() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"status\":\"CONFIRMED\"}")
                .addHeader("Content-Type", "application/json"));

        // When
        boolean result = creditCardPaymentService.verifyPayment("REF123");

        // Then
        assertTrue(result);
    }

    @Test
    void testVerifyPayment_Rejected() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"status\":\"REJECTED\"}")
                .addHeader("Content-Type", "application/json"));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            creditCardPaymentService.verifyPayment("REF123");
        });
        assertTrue(exception.getMessage().contains("rejected"));
    }

    @Test
    void testVerifyPayment_NotFound() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404));

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            creditCardPaymentService.verifyPayment("REF123");
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void testVerifyPaymentFallback() {
        // We test the fallback method directly to ensure it throws the correct
        // exception
        Exception cause = new RuntimeException("Connection failed");

        ExternalServiceException exception = assertThrows(ExternalServiceException.class, () -> {
            creditCardPaymentService.verifyPaymentFallback("REF123", cause);
        });

        assertTrue(exception.getMessage().contains("unavailable"));
    }
}
