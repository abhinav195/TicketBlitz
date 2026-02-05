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
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookTicketRequest request,
                                                         @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(bookingService.bookTicket(request, token));
    }


    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }
}
