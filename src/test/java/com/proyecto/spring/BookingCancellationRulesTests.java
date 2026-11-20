package com.proyecto.spring;

import com.proyecto.spring.business.BookingServiceImpl;
import com.proyecto.spring.business.BusinessException;
import com.proyecto.spring.domain.*;
import com.proyecto.spring.persistence.*;
import com.proyecto.spring.presentation.dto.ApiDtos.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingCancellationRulesTests {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 10, 0);

    @Test
    void cancellationWithinTwentyFourHoursChargesHalfTheRoomPrice() {
        Booking booking = bookingAt(NOW.plusHours(5));
        BookingServiceImpl service = serviceFor(booking);

        var result = service.cancel(booking.getId());

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(new BigDecimal("50.00"), result.cancellationFee());
    }

    @Test
    void cancellationAtOrBeyondTwentyFourHoursIsFree() {
        Booking booking = bookingAt(NOW.plusHours(24));
        BookingServiceImpl service = serviceFor(booking);

        var result = service.cancel(booking.getId());

        assertEquals(BigDecimal.ZERO, result.cancellationFee());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void cancellationLessThanTwoHoursBeforeStartIsRejected() {
        Booking booking = bookingAt(NOW.plusHours(1));
        BookingServiceImpl service = serviceFor(booking);

        BusinessException error = assertThrows(BusinessException.class, () -> service.cancel(booking.getId()));

        assertEquals("RN-08", error.getCodigo());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test
    void cancelledBookingCannotBeConfirmedOrCompleted() {
        Booking booking = bookingAt(NOW.plusHours(24));
        booking.setStatus(BookingStatus.CANCELLED);
        BookingServiceImpl service = serviceFor(booking);

        assertThrows(BusinessException.class, () -> service.confirm(booking.getId()));
        assertThrows(BusinessException.class, () -> service.complete(booking.getId()));
    }

    @Test
    void servicesCannotBeChangedOutsidePendingStatus() {
        Booking booking = bookingAt(NOW.plusHours(24));
        booking.setStatus(BookingStatus.CONFIRMED);
        BookingServiceImpl service = serviceFor(booking);
        BookingItemRequest request = new BookingItemRequest(1L, 1);

        BusinessException addError = assertThrows(BusinessException.class,
                () -> service.addService(booking.getId(), request));
        BusinessException removeError = assertThrows(BusinessException.class,
                () -> service.removeService(booking.getId(), 1L));

        assertEquals("RN-10", addError.getCodigo());
        assertEquals("RN-10", removeError.getCodigo());
    }

    private BookingServiceImpl serviceFor(Booking booking) {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        when(bookingRepository.findDetailedById(booking.getId())).thenReturn(Optional.of(booking));
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new BookingServiceImpl(bookingRepository, mock(RoomRepository.class), mock(MemberRepository.class),
                mock(AdditionalServiceRepository.class), mock(LocationRepository.class), clock);
    }

    private Booking bookingAt(LocalDateTime start) {
        Location location = new Location("Centro", "Ciudad", "Calle 1", LocalTime.of(8, 0), LocalTime.of(22, 0));
        Room room = new Room("Sala", RoomType.MEETING_ROOM, 8, new BigDecimal("40.00"), true, location);
        MembershipPlan plan = new MembershipPlan("Plan", BigDecimal.TEN, BigDecimal.ZERO, 5);
        Member member = new Member("DOC", "Miembro", "m@example.com", plan);
        Booking booking = new Booking(start, start.plusHours(1), 2, room, member, new BigDecimal("100.00"));
        return booking;
    }
}
