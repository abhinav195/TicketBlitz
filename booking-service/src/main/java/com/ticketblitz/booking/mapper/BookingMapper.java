package com.ticketblitz.booking.mapper;

import com.ticketblitz.booking.dto.BookingResponse;
import com.ticketblitz.booking.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    // Maps the internal UUID 'id' to the external 'bookingId' field in the response
    @Mapping(source = "id", target = "bookingId")
    BookingResponse toResponse(Booking booking);
}
