package com.assignments.controller;

import com.assignments.exception.GlobalExceptionHandler;
import com.assignments.model.ReservationEntity;
import com.assignments.model.ReservationRequest;
import com.assignments.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@Import(GlobalExceptionHandler.class)
class ReservationControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ReservationService reservationService;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void testConfirmReservation_Success() throws Exception {
                // Given
                ReservationRequest request = new ReservationRequest();
                request.setCustomerName("Test Customer");
                request.setRoomNumber("101");
                request.setStartDate(LocalDate.now());
                request.setEndDate(LocalDate.now().plusDays(2));
                request.setRoomSegment(ReservationRequest.RoomSegmentEnum.SMALL);
                request.setModeOfPayment(ReservationRequest.ModeOfPaymentEnum.CASH);

                ReservationEntity entity = new ReservationEntity();
                entity.setReservationId("RES-123");
                entity.setStatus(ReservationEntity.ReservationStatus.CONFIRMED);

                when(reservationService.confirmReservation(any(), any(), any(), any(), any(), any(), any()))
                                .thenReturn(entity);

                // When/Then
                mockMvc.perform(post("/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reservationId").value("RES-123"))
                                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        void testConfirmReservation_BadRequest() throws Exception {
                // Given
                ReservationRequest request = new ReservationRequest();
                request.setCustomerName("Test Customer");
                request.setRoomNumber("101");
                request.setStartDate(LocalDate.now());
                request.setEndDate(LocalDate.now().plusDays(2));
                request.setRoomSegment(ReservationRequest.RoomSegmentEnum.SMALL);
                request.setModeOfPayment(ReservationRequest.ModeOfPaymentEnum.CASH);

                when(reservationService.confirmReservation(any(), any(), any(), any(), any(), any(), any()))
                                .thenThrow(new IllegalArgumentException("Invalid reservation duration"));

                // When/Then
                mockMvc.perform(post("/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Bad Request"))
                                .andExpect(jsonPath("$.message").value("Invalid reservation duration"));
        }
}
