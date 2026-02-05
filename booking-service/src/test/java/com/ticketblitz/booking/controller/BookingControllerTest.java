package com.ticketblitz.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketblitz.booking.dto.BookTicketRequest;
import com.ticketblitz.booking.dto.BookingResponse;
import com.ticketblitz.booking.entity.BookingStatus;
import com.ticketblitz.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        BookingController controller = new BookingController();
        ReflectionTestUtils.setField(controller, "bookingService", bookingService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
    }

    // ========================================================================
    // CREATE BOOKING - SUCCESS TESTS
    // ========================================================================

    @Test
    @DisplayName("CreateBooking - Success: Valid request returns 200 and delegates to service")
    void createBooking_validRequest_returns200_andDelegatesToService() throws Exception {
        BookingResponse resp = new BookingResponse();
        resp.setBookingId(123L);
        resp.setUserId(10L);
        resp.setEventId(99L);
        resp.setTicketCount(2);
        resp.setStatus(BookingStatus.PENDING);
        resp.setTotalPrice(new BigDecimal("500.00"));

        when(bookingService.bookTicket(any(BookTicketRequest.class), eq("Bearer token")))
                .thenReturn(resp);

        String json = """
                {
                  "userId": 10,
                  "eventId": 99,
                  "ticketCount": 2
                }
                """;

        mockMvc.perform(
                        post("/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer token")
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookingId").value(123))
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.eventId").value(99))
                .andExpect(jsonPath("$.ticketCount").value(2))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalPrice").value(500.00));

        verify(bookingService).bookTicket(any(BookTicketRequest.class), eq("Bearer token"));
        verifyNoMoreInteractions(bookingService);
    }

    // ========================================================================
    // CREATE BOOKING - VALIDATION FAILURE TESTS
    // ========================================================================

    @Test
    @DisplayName("CreateBooking - Validation Fail: Invalid ticket count (0) returns 400")
    void createBooking_invalidTicketCount_returns400_andDoesNotCallService() throws Exception {
        String json = """
                {
                  "userId": 10,
                  "eventId": 99,
                  "ticketCount": 0
                }
                """;

        mockMvc.perform(
                        post("/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer token")
                                .content(json)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookingService);
    }

    @Test
    @DisplayName("CreateBooking - Validation Fail: Null eventId returns 400")
    void createBooking_nullEventId_returns400_andDoesNotCallService() throws Exception {
        String json = """
                {
                  "userId": 10,
                  "eventId": null,
                  "ticketCount": 1
                }
                """;

        mockMvc.perform(
                        post("/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer token")
                                .content(json)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookingService);
    }

    // ========================================================================
    // GET BOOKING BY ID TESTS
    // ========================================================================

    @Test
    @DisplayName("GetBookingById - Success: Valid ID returns 200")
    void getBookingById_valid_returns200() throws Exception {
        BookingResponse resp = new BookingResponse();
        resp.setBookingId(7L);

        when(bookingService.getBookingById(7L)).thenReturn(resp);

        mockMvc.perform(get("/bookings/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(7));

        verify(bookingService).getBookingById(7L);
        verifyNoMoreInteractions(bookingService);
    }
}
