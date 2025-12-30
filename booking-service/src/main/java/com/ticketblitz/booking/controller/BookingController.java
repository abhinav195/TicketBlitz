package com.ticketblitz.booking.controller;

import com.ticketblitz.booking.dto.BookTicketRequest;
import com.ticketblitz.booking.dto.BookingResponse;
import com.ticketblitz.booking.service.BookingService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    /**
     * The Main Event: Handles high-concurrency booking requests.
     */
    @PostMapping
    public ResponseEntity<BookingResponse> bookTicket(
            @Valid @RequestBody BookTicketRequest request,
            HttpServletRequest httpRequest, // Inject request to get attribute
            @RequestHeader("Authorization") String token
    ) {
        // SECURITY OVERRIDE:
        // Ignore what the hacker sent in request.getUserId()
        // Use the trusted ID from the JWT Interceptor
        Long authenticatedUserId = (Long) httpRequest.getAttribute("authenticatedUserId");
        request.setUserId(authenticatedUserId);

        BookingResponse response = bookingService.bookTicket(request, token);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        // You might need to add this method to your BookingService too
        // if it doesn't exist yet.
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }
}
